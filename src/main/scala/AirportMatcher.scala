package com.agcat.airportstest

import java.io.{File, FileNotFoundException, IOException, PrintWriter}

import com.typesafe.config._
import org.slf4j.LoggerFactory
import redis.clients.jedis.params.geo.GeoRadiusParam
import redis.clients.jedis.{GeoUnit, Jedis}

import scala.io.Source
import scala.util.{Failure, Success, Try}


object ConfigLoader {
  val config = {
    val file = new File("application.conf")

    if (file.exists()) ConfigFactory.parseFile(file) else ConfigFactory.defaultApplication()
  }
}

trait AirportMatcherHelper {
  val logger = LoggerFactory.getLogger(classOf[AirportMatcherHelper])
  val conf = ConfigLoader.config

  def time(block: => Unit): Unit = {
    val t0 = System.currentTimeMillis()
    val result = block

    val t1 = System.currentTimeMillis()
    logger.info("Elapsed time: " + (t1 - t0) + "ms")

    result
  }

  //trying to get files from folder, otherwise loading them from resource
  def getFileOrResource(file: String) =
    try (Source.fromFile(file))
    catch {
      case e: FileNotFoundException => Source.fromResource(file)
      case e: IOException => logger.info("Got an IOException!"); throw e
    }

  //it loads a CSV file into lines
  def loadCSV(file: String, rows: Int = Int.MaxValue) = {
    logger.info(s"Loading file: $file")

    Try {
      val lines = getFileOrResource(file).getLines.drop(1).take(rows).toList

      logger.info(lines.size + " lines loaded from file")
      for {readmeText <- lines} yield readmeText.split(",").toSeq
    }
  }

  /**
    * georadius method can be much more performant if search radius is narrowed.
    * This radius can be defined by configuration. If an airport is not found within this radius,
    * it is multiplied by 2 until airport is found. This method speeds up the calculations considerably
    */
  def closestAirport(jedis: Jedis, lat: Double, lon: Double, radius: Double = Double.MaxValue): String = {
    val places = jedis.georadius("airports", lon, lat, radius, GeoUnit.KM, GeoRadiusParam.geoRadiusParam().sortAscending().count(1))

    if (!places.isEmpty) places.get(0).getMemberByString else closestAirport(jedis, lat, lon, radius * 2)
  }

  def loadAirports(jedis: Jedis) = {
    jedis.flushAll()

    val airports = loadCSV(conf.getString("data.airports"))

    airports match {
      case Success(airports) => airports.foreach { airport => jedis.pipelined().geoadd("airports", airport(2).toDouble, airport(1).toDouble, airport(0)) }
      case Failure(e) => {
        logger.error("There was an error loading the airports")
        throw e
      }

    }

    jedis.resetState()
  }

  def loadUsers = {
    //users file could be partially loaded
    val users =
      if (conf.hasPathOrNull("data.users_rows"))
        loadCSV(conf.getString("data.users"), conf.getInt("data.users_rows"))
      else
        loadCSV(conf.getString("data.users"))

    users match {
      case Success(users) => users
      case Failure(e) => {
        logger.error("There was an error loading the users")
        throw e
      }
    }
  }

  def matchUserAirport(users: List[Seq[String]], jedis: Jedis) = {
    val radius = ConfigLoader.config.getDouble("data.airports_radius")

    //return type is the Tuple (UUID, IATA_CODE)
    users.map(user => (user(0), closestAirport(jedis, user(1).toDouble, user(2).toDouble, radius)))
  }

  def saveOutputFile(users: List[(String, String)]) = {
    val file = new File(ConfigLoader.config.getString("file.output"))
    val writer = new PrintWriter(file)
    val path = file.getAbsolutePath

    logger.info(s"Writing output to $path")

    try {
      writer.println("uuid,iata_code")
      users.foreach(user => writer.println(user.toString().dropRight(1).drop(1)))
    } catch {
      case e: Throwable => logger.error(e.getMessage)
    } finally {
      writer.close()
    }
  }
}

object AirportMatcher extends App with AirportMatcherHelper {
  val (ip, port) = (conf.getString("redis.ip"), conf.getInt("redis.port"))
  logger.info(s"Trying to connect Jedis Server - IP address: $ip Port: $port")

  val jedis = new Jedis(ip, port)

  loadAirports(jedis)
  val users = loadUsers

  time {
    val output = matchUserAirport(users, jedis)

    saveOutputFile(output)
  }

}