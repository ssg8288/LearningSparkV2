package main.scala.chapter8

import org.apache.spark.sql.SparkSession
import org.apache.spark.storage.StorageLevel

object CachingData_8_5 {

	def printConfig(session: SparkSession, key:String) = {
		// get conf
		val v = session.conf.getOption(key)
		println(s"${key} -> ${v}\n")
	}

	def timer[A](blockOfCode: => A) = {
		val startTime = System.nanoTime
		val result = blockOfCode
		val endTime = System.nanoTime
		val delta = endTime - startTime
		(result, delta/1000000d)

	}

	def main(args: Array[String]) {
		// create a session
		val spark = SparkSession.builder
  			.master("local[*]")
				.appName("CachingData")
				.getOrCreate()

		import spark.implicits._

		printConfig(spark, "\"spark.sql.join.preferSortMergeJoin\"")
		val df = spark.range(1 * 10000000).toDF("id").withColumn("square", $"id" * $"id")
	  // cache DataFrame
		df.cache()
	  val (res, tm) = timer(df.count())
		println(s"***** Count=${res} and time=${tm}")
		println("***** Get the second time around")
		val (res2, tm2) = timer(df.count())
		println(s"***** Count=${res2} and time=${tm2}")
		// Persist on Disk
		df.persist(StorageLevel.DISK_ONLY)
		val (res3, tm3) = timer(df.count())
		println(s"***** Count=${res3} and time=${tm3}")
		// unpersist
		df.unpersist()
		// create an temporary SQL view
		df.createOrReplaceTempView("dfTable")
		spark.sql("cache table dfTable")
		val (res4, tm4) = timer(spark.sql("select count(*) from dfTable").show())
		println(s"***** Count=${res4} and time=${tm4}")


	}
}