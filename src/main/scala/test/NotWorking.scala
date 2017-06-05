package test

import java.util.concurrent.atomic.AtomicLong

import com.timgroup.statsd.NonBlockingStatsDClient
import com.typesafe.scalalogging.LazyLogging
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.spark.SparkConf
import org.apache.spark.rdd.RDD
import org.apache.spark.streaming.dstream.DStream
import org.apache.spark.streaming.kafka010.ConsumerStrategies.Subscribe
import org.apache.spark.streaming.kafka010.LocationStrategies.PreferConsistent
import org.apache.spark.streaming.kafka010.{CanCommitOffsets, HasOffsetRanges, KafkaUtils}
import org.apache.spark.streaming.{Seconds, StreamingContext}


class NotWorking extends LazyLogging {

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
        import argonaut.Argonaut.StringToParseWrap

        val rdd: RDD[SimpleData] = kafkaRdd.mapPartitions((records: Iterator[ConsumerRecord[String, String]]) => {
          val invalidCount: AtomicLong = new AtomicLong(0)
          val convertedData: Iterator[SimpleData] = records.map(record => {
            val maybeData: Option[SimpleData] = record.value().decodeOption[SimpleData]
            if (maybeData.isEmpty) {
              logger.error("Cannot parse data from kafka: " + record.value())
              invalidCount.incrementAndGet()
            }
            maybeData
          })
            .filter(_.isDefined)
            .map(_.get)

          val statsDClient = new NonBlockingStatsDClient("appName", "monitoring.host", 8125) // I know it should be a singleton :)
          statsDClient.gauge("invalid-input-records", invalidCount.get())

          convertedData
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

