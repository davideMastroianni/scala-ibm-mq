package it.dmastro.queue.resources.mongo.collections

import org.mongodb.scala.model.IndexModel

trait MongoCollectionInfo {
  def getIndex: Seq[IndexModel]

  def getName: String
}
