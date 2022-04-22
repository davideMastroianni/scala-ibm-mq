package it.dmastro.queue.configuration

import cats.effect.IO
import com.typesafe.config.Config

import scala.jdk.CollectionConverters._

object MongoDBConfiguration {

  def configure(config: Config): IO[MongoDBConfiguration] = {
    IO {
      val muser         = config.getString("mongo.username")
      val mpassword     = config.getString("mongo.password")
      val mport         = config.getInt("mongo.port")
      val endpoints     = Endpoint.from(
        config.getStringList("mongo.endpoints").asScala.toList,
        mport
      )
      val authrequired  = config.getBoolean("mongo.authrequired")
      val sslEnabled    = config.getBoolean("mongo.ssl")

      MongoDBConfiguration(
        authRequired = authrequired,
        username     = Username(muser),
        password     =  Password(mpassword),
        endpoints    = endpoints,
        sslEnabled   = sslEnabled
      )
    }
  }

}

case class MongoDBName(value: String) extends AnyVal

case class Username(value: String) extends AnyVal

case class Password(value: String) extends AnyVal

case class Endpoint(host: String, port: Int)

object Endpoint {
  def from(s: List[String], port: Int): List[Endpoint] =
    s.map(Endpoint(_, port))
}

case class MongoDBConfiguration(
  authRequired: Boolean,
  username: Username,
  password: Password,
  endpoints: List[Endpoint],
  database: MongoDBName = MongoDBName("oms-flow-comparator"),
  sslEnabled: Boolean
)