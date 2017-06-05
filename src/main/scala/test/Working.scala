package test

import com.timgroup.statsd.NonBlockingStatsDClient
import com.typesafe.scalalogging.LazyLogging
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.spark.SparkConf
import org.apache.spark.rdd.RDD
import org.apache.spark.streaming.{Seconds, StreamingContext}
import org.apache.spark.streaming.dstream.DStream
import org.apache.spark.streaming.kafka010.ConsumerStrategies.Subscribe
import org.apache.spark.streaming.kafka010.{CanCommitOffsets, HasOffsetRanges, KafkaUtils}
import org.apache.spark.streaming.kafka010.LocationStrategies.PreferConsistent


class Working extends LazyLogging {

  def creatingFunc(): StreamingContext = {
    val sparkConf = new SparkConf()
    val ssc = new StreamingContext(sparkConf, Seconds(10))

    val stream: DStream[ConsumerRecord[String, String]] = KafkaUtils.createDirectStream[String, String](
      ssc,
      PreferConsistent,
      Subscribe[String, String](Set("mytopic"), Map("setting1" -> "value1")
      )
    )

    stream
      .foreachRDD((kafkaRdd: RDD[ConsumerRecord[String, String]]) => {
        val offsetRanges = kafkaRdd.asInstanceOf[HasOffsetRanges].offsetRanges

        val initialRecords = kafkaRdd.count()
        logger.info(s"Initial records: $initialRecords")

        // this is ugly we have to repeat it - but argonaut is NOT serializable...
        val rdd: RDD[SimpleData] = kafkaRdd.mapPartitions((records: Iterator[ConsumerRecord[String, String]]) => {
          import argonaut.Argonaut.StringToParseWrap
          val convertedDataTest: Iterator[(Option[SimpleData], String)] = records.map(record => {
            val maybeData: Option[SimpleData] = record.value().decodeOption[SimpleData]
            (maybeData, record.value())
          })

          val testInvalidDataEntries: Int = convertedDataTest.count(record => {
            val empty = record._1.isEmpty
            if (empty) {
              logger.error("Cannot parse data from kafka: " + record._2)
            }
            empty
          })
          val statsDClient = new NonBlockingStatsDClient("appName", "monitoring.host", 8125) // I know it should be a singleton :)
          statsDClient.gauge("invalid-input-records", testInvalidDataEntries)

          convertedDataTest
            .filter(maybeData => maybeData._1.isDefined)
            .map(data => data._1.get)
        })

        rdd.collect().length
        stream.asInstanceOf[CanCommitOffsets].commitAsync(offsetRanges)
      })

    ssc
  }

  val ssc: StreamingContext = StreamingContext.getActiveOrCreate(creatingFunc)

  // Start the computation
  ssc.start()
  ssc.awaitTermination()
  logger.info("Context stopped")
  System.exit(0)
}

