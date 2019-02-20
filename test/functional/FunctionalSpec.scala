package functional

import controllers.MainController
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.test._
/**
 * Functional specification that has a running Play application.
 *
 * This is good for testing filter functionality, such as CSRF token and template checks.
 *
 * See https://www.playframework.com/documentation/2.6.x/ScalaFunctionalTestingWithScalaTest for more details.
 */
class FunctionalSpec extends PlaySpec with GuiceOneAppPerSuite with Injecting with ScalaFutures {

  // CSRF token helper adds "withCSRFToken" to FakeRequest:
  // https://www.playframework.com/documentation/2.6.x/ScalaCsrf#Testing-CSRF
  import CSRFTokenHelper._

  "LobbyController" must {

    "process a POST request successfully" in {
      // Pull the controller from the already running Play application, using Injecting
      val controller = inject[MainController]

      // TODO functional tests
    }

    "reject a POST request when given bad input" in {
      // Pull the controller from the already running Play application, using Injecting
      val controller = inject[MainController]

      // TODO functional tests
    }
  }

}
