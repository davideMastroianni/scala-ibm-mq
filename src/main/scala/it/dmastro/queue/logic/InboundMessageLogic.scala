package it.dmastro.queue.logic

import cats.effect.IO
import it.dmastro.queue.model.{Result, Success}
import it.dmastro.queue.resources.mongo.MongoStore
import jms4s.jms.JmsMessage
import org.mongodb.scala.Document

import java.util.UUID

class InboundMessageLogic private (mongoStore: MongoStore) extends AbstractLogic {

  override def describe(message: JmsMessage): IO[Either[Throwable, Result]] =
    findMessage(message) match {
      case Right()
    }

  def insertMessage(message: JmsMessage): IO[Either[Throwable, Result]] =
    (for {
      insert <- mongoStore.insert(
        Document(
          "_id" -> "random_id",
          "message" -> message.attemptAsText.get
        ))
    } yield Success(message, insert)).attempt

  def findMessage(message: JmsMessage): IO[Either[Throwable, Result]] =
    mongoStore.find("random_id").map(_ => Success(message, _)).attempt
}
