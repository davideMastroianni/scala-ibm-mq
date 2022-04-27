package it.dmastro.queue.logic

import cats.effect.IO
import it.dmastro.queue.decoder.InboundMessageDecoder.InboundMessageDecoder
import it.dmastro.queue.model.{InboundXmlMessage, Result, Success}
import it.dmastro.queue.resources.mongo.MongoStore
import jms4s.jms.JmsMessage
import org.mongodb.scala.bson.collection.immutable.Document

class InboundMessageLogic private (mongoStore: MongoStore)
    extends AbstractLogic {

  override def describe(message: JmsMessage): IO[Either[Throwable, Result]] = {
    (for {
      decodedMessage <- message.decode
      maybeDocument <- mongoStore.findFirst(decodedMessage.toFindFilters)
      upsertMessage <- upsertInboundMessage(decodedMessage, maybeDocument)
    } yield Success(message, upsertMessage)).attempt
  }

  private def upsertInboundMessage(
      decodedMessage: InboundXmlMessage,
      maybeDocument: Option[Document]
  ): IO[Document] =
    if (maybeDocument.isEmpty)
      mongoStore.insert(decodedMessage.toMongoDocument)
    else
      mongoStore.updateOne(decodedMessage.toUpdateOneModel)

}

object InboundMessageLogic {
  def make(mongoStore: MongoStore): InboundMessageLogic =
    new InboundMessageLogic(mongoStore);
}
