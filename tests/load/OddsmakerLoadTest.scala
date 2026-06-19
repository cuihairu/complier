package tests.load

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._

/**
 * Oddsmaker Control Service Load Test
 *
 * This test simulates realistic user behavior on the Oddsmaker platform.
 *
 * Run with:
 *   ./gradlew gatlingRun -Dgatling.simulationClass=tests.load.OddsmakerLoadTest
 */
class OddsmakerLoadTest extends Simulation {

  // Configuration
  val baseUrl = System.getProperty("base.url", "http://localhost:8085")
  val adminToken = System.getProperty("admin.token", "dev-admin-token-change-in-production")
  val rampUpDuration = System.getProperty("ramp.up.seconds", "60").toInt.seconds
  val testDuration = System.getProperty("test.duration.seconds", "300").toInt.seconds
  val targetRps = System.getProperty("target.rps", "100").toInt

  // HTTP configuration
  val httpProtocol = http
    .baseUrl(baseUrl)
    .acceptHeader("application/json")
    .contentTypeHeader("application/json")
    .header("Authorization", s"Bearer $adminToken")
    .userAgentHeader("Oddsmaker-LoadTest/1.0")

  // Test data
  val gameIds = Iterator.continually(Map("gameId" -> s"game_${scala.util.Random.alphanumeric.take(8).mkString}"))

  // Scenarios

  // Scenario 1: Game Management
  val gameManagementScenario = scenario("Game Management")
    .exec(
      http("Create Game")
        .post("/api/games")
        .body(StringBody("""{"name":"Load Test Game","genre":"rpg","platforms":["android","ios"],"timezone":"UTC","defaultCurrency":"USD"}"""))
        .check(status.is(200))
        .check(jsonPath("$.id").saveAs("gameId"))
    )
    .pause(1.second)
    .exec(
      http("Get Game")
        .get("/api/games/${gameId}")
        .check(status.is(200))
        .check(jsonPath("$.name").is("Load Test Game"))
    )
    .pause(500.milliseconds)
    .exec(
      http("List Games")
        .get("/api/games")
        .check(status.is(200))
        .check(jsonPath("$.items").exists)
    )
    .pause(500.milliseconds)
    .exec(
      http("Update Game")
        .put("/api/games/${gameId}")
        .body(StringBody("""{"name":"Updated Load Test Game"}"""))
        .check(status.is(200))
    )
    .pause(1.second)
    .exec(
      http("Delete Game")
        .delete("/api/games/${gameId}")
        .check(status.is(200))
    )

  // Scenario 2: Environment Management
  val environmentScenario = scenario("Environment Management")
    .feed(gameIds)
    .exec(
      http("Create Game for Environment Test")
        .post("/api/games")
        .body(StringBody("""{"name":"Env Test Game ${gameId}","genre":"strategy","platforms":["web"],"timezone":"UTC"}"""))
        .check(status.is(200))
        .check(jsonPath("$.id").saveAs("envGameId"))
    )
    .pause(500.milliseconds)
    .exec(
      http("Create Environment")
        .post("/api/games/${envGameId}/environments")
        .body(StringBody("""{"name":"staging","type":"STAGING","displayName":"Staging Environment"}"""))
        .check(status.is(200))
    )
    .pause(500.milliseconds)
    .exec(
      http("List Environments")
        .get("/api/games/${envGameId}/environments")
        .check(status.is(200))
    )
    .pause(500.milliseconds)
    .exec(
      http("Get Environment")
        .get("/api/games/${envGameId}/environments/staging")
        .check(status.is(200))
    )

  // Scenario 3: API Key Management
  val apiKeyScenario = scenario("API Key Management")
    .feed(gameIds)
    .exec(
      http("Create Game for API Key Test")
        .post("/api/games")
        .body(StringBody("""{"name":"API Key Test Game ${gameId}","genre":"casual","platforms":["android"],"timezone":"UTC"}"""))
        .check(status.is(200))
        .check(jsonPath("$.id").saveAs("apiKeyGameId"))
    )
    .pause(500.milliseconds)
    .exec(
      http("Create API Key")
        .post("/api/api-keys")
        .body(StringBody("""{"gameId":"${apiKeyGameId}","name":"test-key","keyType":"client"}"""))
        .check(status.is(200))
        .check(jsonPath("$.publicKey").saveAs("apiKey"))
    )
    .pause(500.milliseconds)
    .exec(
      http("Get API Key")
        .get("/api/api-keys/${apiKey}")
        .check(status.is(200))
    )
    .pause(500.milliseconds)
    .exec(
      http("List API Keys")
        .get("/api/api-keys?gameId=${apiKeyGameId}")
        .check(status.is(200))
    )
    .pause(500.milliseconds)
    .exec(
      http("Delete API Key")
        .delete("/api/api-keys/${apiKey}")
        .check(status.is(200))
    )

  // Scenario 4: Experiment Management
  val experimentScenario = scenario("Experiment Management")
    .feed(gameIds)
    .exec(
      http("Create Game for Experiment Test")
        .post("/api/games")
        .body(StringBody("""{"name":"Experiment Test Game ${gameId}","genre":"rpg","platforms":["ios"],"timezone":"UTC"}"""))
        .check(status.is(200))
        .check(jsonPath("$.id").saveAs("expGameId"))
    )
    .pause(500.milliseconds)
    .exec(
      http("Create Experiment")
        .post("/api/experiments")
        .body(StringBody("""{"gameId":"${expGameId}","name":"Test Experiment","status":"draft","salt":"test-salt","config":{"variants":[{"name":"A","weight":50},{"name":"B","weight":50}],"metrics":{"primary":"conversion"}}}"""))
        .check(status.is(200))
        .check(jsonPath("$.id").saveAs("expId"))
    )
    .pause(500.milliseconds)
    .exec(
      http("Get Experiment")
        .get("/api/experiments/${expId}")
        .check(status.is(200))
    )
    .pause(500.milliseconds)
    .exec(
      http("List Experiments")
        .get("/api/experiments?gameId=${expGameId}")
        .check(status.is(200))
    )
    .pause(500.milliseconds)
    .exec(
      http("Publish Experiment")
        .post("/api/experiments/${expId}/publish")
        .check(status.is(200))
    )
    .pause(500.milliseconds)
    .exec(
      http("Pause Experiment")
        .post("/api/experiments/${expId}/pause")
        .check(status.is(200))
    )
    .pause(500.milliseconds)
    .exec(
      http("Delete Experiment")
        .delete("/api/experiments/${expId}")
        .check(status.is(200))
    )

  // Scenario 5: Health Check
  val healthCheckScenario = scenario("Health Check")
    .exec(
      http("Health Check")
        .get("/actuator/health")
        .check(status.is(200))
        .check(jsonPath("$.status").is("UP"))
    )
    .pause(1.second)
    .exec(
      http("Detailed Health")
        .get("/actuator/health/detail")
        .check(status.is(200))
    )

  // Scenario 6: Read-Heavy Operations
  val readHeavyScenario = scenario("Read Heavy Operations")
    .feed(gameIds)
    .exec(
      http("List Games (Read)")
        .get("/api/games")
        .check(status.is(200))
    )
    .pause(100.milliseconds)
    .exec(
      http("Search Games (Read)")
        .get("/api/games?q=test")
        .check(status.is(200))
    )
    .pause(100.milliseconds)
    .exec(
      http("List Experiments (Read)")
        .get("/api/experiments")
        .check(status.is(200))
    )
    .pause(100.milliseconds)
    .exec(
      http("Health Check (Read)")
        .get("/actuator/health")
        .check(status.is(200))
    )

  // Load profile
  setUp(
    // Ramp up
    gameManagementScenario.inject(
      rampUsers(10).during(rampUpDuration),
      constantUsersPerSec(5).during(testDuration)
    ),
    environmentScenario.inject(
      rampUsers(5).during(rampUpDuration),
      constantUsersPerSec(2).during(testDuration)
    ),
    apiKeyScenario.inject(
      rampUsers(5).during(rampUpDuration),
      constantUsersPerSec(2).during(testDuration)
    ),
    experimentScenario.inject(
      rampUsers(3).during(rampUpDuration),
      constantUsersPerSec(1).during(testDuration)
    ),
    healthCheckScenario.inject(
      rampUsers(20).during(rampUpDuration),
      constantUsersPerSec(10).during(testDuration)
    ),
    readHeavyScenario.inject(
      rampUsers(50).during(rampUpDuration),
      constantUsersPerSec(25).during(testDuration)
    )
  ).protocols(httpProtocol)
    .assertions(
      global.responseTime.max.lt(5000),
      global.responseTime.mean.lt(1000),
      global.responseTime.percentile3.lt(2000),
      global.successfulRequests.percent.gt(95.0)
    )
}
