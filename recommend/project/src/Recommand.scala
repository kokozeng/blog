package org.myorg
import java.io.File

import scala.io.Source
import org.apache.log4j.Logger
import org.apache.log4j.Level
import org.apache.spark.SparkConf
import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.mllib.recommendation.{ALS, MatrixFactorizationModel, Rating}
import org.apache.spark.rdd._

/**
  * Created by Administrator on 2018/1/9.
  */
object Recommand {
  def main (args:Array[String]) {
  SetLogger
  println("-----the period of data loding-----")
  val (ratings, movieTitle) = PrepareData(args)
  println("-----the period of training-----")
  println("beging use" + ratings.count() + "data begin train model...")
  val model = ALS.train(ratings, 5, 20, 0.1)
  println("finish")
  println("the period of recommand")
  recommend(model, movieTitle)
  println("finish")

  }


  def recommend(model: MatrixFactorizationModel, movieTitle: Map[Int, String]) = {
    var choose = ""
    while (choose != "3") {
      print("choose: 1.recommand for user 2.recommand for movies 3.leave?")
      choose = Console.readLine()
      if (choose == "1") {
        print("please input your user id?")
        val inputUserID = readLine()
        RecommendMovies(model, movieTitle, inputUserID.toString.toInt)
      }else if (choose == "2"){
        print("please input your movie id?")
        val inputMovieID = readLine()
        RecommendMovies(model, movieTitle, inputMovieID.toString.toInt)

      }
    }

  }


  def SetLogger = {
    Logger.getLogger("org").setLevel(Level.OFF)
    Logger.getLogger("com").setLevel(Level.OFF)
    System.setProperty("spark.ui.showConsoleProgress", "false")
    Logger.getRootLogger().setLevel(Level.OFF);
  }

  def PrepareData(args:Array[String]): (RDD[Rating], Map[Int, String]) = {
    val sc = new SparkContext(new SparkConf().setAppName("RDF").setMaster("local[4]"))
    print("reading user's score...")
    val rawUserData = sc.textFile(args(0))
    val rawRating = rawUserData.map(_.split("\t").take(3))
    val ratingsRDD = rawRating.map{ case Array(user, movie, rating) => Rating(user.toInt, movie.toInt, rating.toDouble)}
    println("total:" + ratingsRDD.count.toString() + "ratings")
    print("reading movie lists...")
    val itemRDD = sc.textFile(args(1))
    val movieTitle = itemRDD.map(line => line.split("\\|").take(2))
        .map(array => (array(0).toInt, array(1))).collect().toMap
    val numRatings = ratingsRDD.count()
    val numUsers = ratingsRDD.map(_.user).distinct().count()
    val numMovies = ratingsRDD.map(_.product).distinct().count()
    println("total:ratings:" + numRatings + "user" + numUsers + "Movie" + numMovies)
    return (ratingsRDD, movieTitle)
  }

  def RecommendMovies(model: MatrixFactorizationModel, movieTitle:Map[Int, String], inputUserID: Int) = {
    val RecommendMovie = model.recommendProducts(inputUserID, 10)
    var i =1
    println("for user id" + inputUserID + "recommend this movies:")
    RecommendMovie.foreach{ r =>
      println(i.toString() + "." + movieTitle(r.product) + "score:" + r.rating.toString())
      i += 1

    }
  }

  def RecommendUsers(model: MatrixFactorizationModel, movieTitle:Map[Int, String], inputMovieID: Int) = {
    val RecommendUser = model.recommendUsers(inputMovieID, 10)
    var i =1
    println("for movie id" + inputMovieID + "movie name" + movieTitle(inputMovieID.toInt) + "recommand this users id:")
    RecommendUser.foreach { r =>
      println(i.toString + "user id" + r.user + "score:" + r.rating)
      i = i + 1
    }

    }
}
