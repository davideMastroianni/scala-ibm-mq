package it.dmastro.queue.model

import cats.Show
import cats.implicits.toShow
import jms4s.jms.JmsMessage
import org.mongodb.scala.Document

sealed trait Result

case class NoOp(message: JmsMessage, description: String) extends Result

object NoOp {

  implicit val show: Show[NoOp] = Show.show(
    success => s"Message ignored:\n ${success.message.show} \n ${success.description}"
  )

}

case class Success(message: JmsMessage, query: Document) extends Result

object Success {

  implicit val show: Show[Success] = Show.show(
    success =>
      s"Message stored with input: \n ${success.message.show} \n and query:\n ${success.query.toString()}"
  )
}
