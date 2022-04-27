package it.dmastro.queue.configuration

import cats.data.NonEmptyList
import cats.effect.IO
import com.typesafe.config.Config
import jms4s.ibmmq.ibmMQ.{
  Channel,
  ClientId,
  Endpoint,
  Password,
  QueueManager,
  Username,
  Config => JmsConfig
}

import scala.jdk.CollectionConverters._

object JmsConfiguration {

  def configure(config: Config): IO[JmsConfig] =
    IO {
      val qmUserName = config.getString("jms.queueManager.username")
      val qmPassword = config.getString("jms.queueManager.password")
      val authRequired = config.getBoolean("jms.queueManager.authrequired")
      val name = config.getString("jms.queueManager.name")
      val endpoints = config
        .getConfigList("jms.queueManager.endpoints")
        .asScala
        .map(endpoint => {
          Endpoint(
            endpoint.getString("host"),
            endpoint.getInt("port")
          )
        })
        .toList

      JmsConfig(
        qm = QueueManager(name),
        endpoints = NonEmptyList.fromListUnsafe(endpoints),
        channel = Channel("MQCHL.CLT"),
        username = Some(Username(qmUserName)),
        password = if (authRequired) Some(Password(qmPassword)) else None,
        clientId = ClientId("ibm-mq-app")
      )
    }

}
