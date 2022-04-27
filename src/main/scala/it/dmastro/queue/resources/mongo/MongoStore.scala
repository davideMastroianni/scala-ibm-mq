package it.dmastro.queue.resources.mongo

import cats.effect.{IO, Resource}
import it.dmastro.queue.configuration.MongoDBConfiguration
import it.dmastro.queue.resources.mongo.collections.MongoCollectionInfo
import it.dmastro.queue.utils.Utils.{ObservableOps, SingleObservableOps}
import org.mongodb.scala.{
  MongoClient,
  MongoClientSettings,
  MongoCollection,
  MongoCredential,
  ServerAddress
}
import org.mongodb.scala.bson.collection.immutable.Document
import org.mongodb.scala.bson.conversions.Bson
import org.mongodb.scala.model.{
  FindOneAndUpdateOptions,
  ReturnDocument,
  UpdateOneModel
}
import org.typelevel.log4cats.Logger

import java.util.concurrent.TimeUnit
import scala.jdk.CollectionConverters._

class MongoStore(collection: MongoCollection[Document])(implicit
    logger: Logger[IO]
) {

  def insert(record: Document): IO[Document] =
    collection
      .insertOne(record)
      .toIO
      .map(_ => record)

  def findFirst(filters: Bson): IO[Option[Document]] =
    collection
      .find(filters)
      .toIO
      .map(_.headOption)

  def updateOne(
      update: UpdateOneModel[_ <: Document]
  ): IO[Document] = {
    collection
      .findOneAndUpdate(
        update.getFilter,
        update.getUpdate,
        FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER)
      )
      .toIO
  }
}

object MongoStore {

  def resource(
      mongoDBConfiguration: MongoDBConfiguration,
      client: MongoClient,
      collectionInfo: MongoCollectionInfo
  )(implicit
      logger: Logger[IO]
  ): IO[MongoStore] =
    for {
      collection <- IO(
        client
          .getDatabase(mongoDBConfiguration.database.value)
          .getCollection(collectionInfo.getName)
      )
      _ <- collection.createIndexes(collectionInfo.getIndex).toIO
    } yield new MongoStore(collection)

  def makeClient(conf: MongoDBConfiguration): Resource[IO, MongoClient] =
    Resource.fromAutoCloseable {
      IO {
        val addresses: List[ServerAddress] =
          conf.endpoints.map(end => new ServerAddress(end.host, end.port))
        val maybeCredential: Option[MongoCredential] =
          if (conf.authRequired)
            Some(
              MongoCredential.createScramSha1Credential(
                conf.username.value,
                "admin",
                conf.password.value.toCharArray
              )
            )
          else None

        val settings = MongoClientSettings
          .builder()
          .applyToClusterSettings { t =>
            t.hosts(addresses.asJava)
            ()
          }
          .applyToSocketSettings { t =>
            t.readTimeout(30, TimeUnit.SECONDS)
            ()
          }
          .applyToSslSettings { t =>
            t.enabled(conf.sslEnabled)
            ()
          }
          .retryWrites(true)

        maybeCredential match {
          case Some(credentials) =>
            MongoClient(settings.credential(credentials).build())
          case None => MongoClient(settings.build())
        }
      }
    }
}
