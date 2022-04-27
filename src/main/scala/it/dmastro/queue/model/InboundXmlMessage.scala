package it.dmastro.queue.model

import com.mongodb.client.model.UpdateOneModel
import org.mongodb.scala.bson.collection.immutable.Document
import org.mongodb.scala.bson.conversions.Bson
import org.mongodb.scala.model.Filters.equal
import org.mongodb.scala.model.Updates.push

case class InboundXmlMessage(
    timestamp: Long,
    messageId: String,
    inboundMessageId: String,
    xml: String
) {

  def toUpdateOneModel: UpdateOneModel[_ <: Document] = {
    val filters = toFindFilters

    val update = push(
      "messageList",
      Document("messageId" -> this.messageId, "xml" -> this.xml)
    )

    new UpdateOneModel(filters, update)
  }

  def toMongoDocument: Document = {
    Document(
      "inboundMessageId" -> this.inboundMessageId,
      "timestamp" -> this.timestamp,
      "messageList" -> List(
        Document(
          "messageId" -> this.messageId,
          "xml" -> this.xml
        )
      )
    )
  }

  def toFindFilters: Bson =
    equal("inboundMessageId", this.inboundMessageId)

}

object InboundXmlMessage {

  def from(
      text: String,
      timestamp: Long,
      messageId: String,
      inboundMessageId: String
  ): InboundXmlMessage =
    InboundXmlMessage(timestamp, messageId, inboundMessageId, text)

}
