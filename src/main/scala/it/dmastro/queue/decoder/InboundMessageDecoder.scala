package it.dmastro.queue.decoder

import cats.effect.IO
import it.dmastro.queue.model.InboundXmlMessage
import jms4s.jms.JmsMessage

import scala.util.Try
import scala.xml.XML

object InboundMessageDecoder {

  implicit class InboundMessageDecoder(message: JmsMessage) {

    def getInboundMessageId(text: String): Try[String] = {
      Try(text)
        .map(XML.loadString)
        .map(xml => xml \@ "OrderNo")
    }

    def decode: IO[InboundXmlMessage] = {
      for {
        msg <- message.asJmsTextMessageF[IO]
        text <- IO.fromTry(msg.getText)
        id <- IO.fromTry(getInboundMessageId(text))
        timestamp <- message.getJMSTimestamp
          .fold[IO[Long]](
            IO.raiseError(new RuntimeException("Missing JMSTimestamp"))
          )(IO.pure)
        messageId <- message.getJMSMessageId
          .fold[IO[String]](
            IO.raiseError(new RuntimeException("Missing JMSMessageId"))
          )(IO.pure)
        res <- IO(InboundXmlMessage.from(text, timestamp, messageId, id))
      } yield res
    }.handleErrorWith(t =>
      IO.raiseError(new Exception("Failed to decode input xml", t))
    )

  }

}
