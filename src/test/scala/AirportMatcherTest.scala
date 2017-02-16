import com.agcat.airportstest.AirportMatcherHelper
import org.junit.runner.RunWith
import org.scalatest.Inspectors._
import org.scalatest.junit.JUnitRunner
import org.scalatest.{FunSuite, Matchers}
import redis.clients.jedis.Jedis

@RunWith(classOf[JUnitRunner])
class AirportMatcherTest extends org.scalatest.FunSuite with AirportMatcherHelper with Matchers {
  val jedis = new Jedis(conf.getString("redis.ip"), conf.getInt("redis.port"))

  //ideally it should be read from a resource file inside Test Resource Folder
  val testSet = Seq(
    (-37.83330154418945, 145.0500030517578, "MBW"),
    (40.41650009155273, -3.702600002288818, "MAD"),
    (48.86370086669922, 2.276900053024292, "VIY"),
    (41.009778, 0.619723, "REU"),
    (40.9364013671875, -74.11840057373047, "TEB"),
    (48.76670074462891, 9.183300018310547, "STR"),
    (52.30580139160156, 5.01669979095459, "AMS"),
    (-33.86780166625977, 151.2073059082031, "RSE"),
    (51.18330001831055, 9.316699981689453, "FRZ")
  )

  test("Test Airports file Exists") {
    noException should be thrownBy getFileOrResource(conf.getString("data.airports")).size
  }

  test("Test Users file Exists") {
    noException should be thrownBy getFileOrResource(conf.getString("data.users")).size
  }

  test("Test Closest Airport") {
    //if it fails, it gets the failing element like:
    // at index n, false was not true
    forAll(testSet) { user => closestAirport(jedis, user._1, user._2) == user._3 should be(true) }
  }

}
