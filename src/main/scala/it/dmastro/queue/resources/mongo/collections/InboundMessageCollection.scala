package it.dmastro.queue.resources.mongo.collections
import org.mongodb.scala.model.{IndexModel, IndexOptions, Indexes}

import java.util.concurrent.TimeUnit

class InboundMessageCollection extends MongoCollectionInfo {

  override def getIndex: Seq[IndexModel] =
    Seq(
      IndexModel(
        Indexes.ascending("inboundMessageId"),
        IndexOptions()
          .name("inboundMessageIdIndex")
          .unique(true)
          .background(false)
      ),
      IndexModel(
        Indexes.ascending("createdDateTime"),
        IndexOptions()
          .name("createdDateTimeIndex")
          .unique(false)
          .background(false)
          .expireAfter(730L, TimeUnit.DAYS)
      )
    )

  override def getName: String = "inbound_message"
}
