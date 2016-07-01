package se.uu.farmbio.easymr

import scala.io.Source

import org.apache.spark.SparkConf
import org.apache.spark.SparkContext
import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner

import com.google.common.io.Files
import java.io.PrintWriter
import org.scalatest.BeforeAndAfterAll
import scala.reflect.io.Path
import org.apache.commons.io.FileUtils


@RunWith(classOf[JUnitRunner])
class EasyMapReduceTest extends FunSuite with BeforeAndAfterAll {

  val conf = new SparkConf()
    .setAppName("MapReduceTest")
    .setMaster("local[*]")

  val tempDir = Files.createTempDir

  test("easy map") {

    val params = EasyMapParams(
      command = "rev <input> > <output>",
      imageName = "ubuntu:14.04",
      local = true,
      inputPath = getClass.getResource("dna.txt").getPath,
      outputPath = tempDir.getAbsolutePath + "/rev.txt",
      fifoReadTimeout = 30)
    EasyMap.run(params)

    val reverseTest = Source.fromFile(getClass.getResource("dna.txt").getPath)
      .getLines.map(_.reverse)

    val sc = new SparkContext(conf)
    val reverseOut = sc.textFile(tempDir.getAbsolutePath + "/rev.txt").collect
    sc.stop

    assert(reverseTest.toSet == reverseOut.toSet)

  }

  test("easy reduce") {
    
    //Make a test input from DNA
    val lineCount = Source.fromFile(getClass.getResource("dna.txt").getPath).getLines
      .map(_.filter(n => n == 'g' || n == 'c').length)    
    new PrintWriter(tempDir.getAbsolutePath + "/count_by_line.txt") { 
      write(lineCount.mkString("\n"))
      close 
    }
    
    val params = EasyReduceParams(
      command = "expr $(cat <input:1>) + $(cat <input:2>) > <output>",
      imageName = "ubuntu:14.04",
      local = true,
      inputPath = tempDir.getAbsolutePath + "/count_by_line.txt",
      outputPath = tempDir.getAbsolutePath + "/sum.txt",
      fifoReadTimeout = 30)
    EasyReduce.run(params)
    
    val sc = new SparkContext(conf)
    val sumOut = sc.textFile(tempDir.getAbsolutePath + "/sum.txt").first
    sc.stop 
    
    val sumTest = Source.fromFile(tempDir.getAbsolutePath + "/count_by_line.txt")
      .getLines.map(_.toInt).sum
    
    assert(sumOut.toInt == sumTest)
    
  }
  
  override def afterAll {
    FileUtils.deleteDirectory(tempDir)
  }

}