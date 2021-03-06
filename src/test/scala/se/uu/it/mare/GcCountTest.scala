package se.uu.it.mare

import org.apache.spark.SharedSparkContext
import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class GcCountTest extends FunSuite with SharedSparkContext {

  test("GC count in DNA string, defaults") {

    val rdd = sc.textFile(getClass.getResource("dna/dna.txt").getPath)

    val res = new MaRe(rdd)
      .mapPartitions(
        imageName = "ubuntu:xenial",
        command = "grep -o '[gc]' /input | wc -l > /output")
      .reducePartitions(
        imageName = "ubuntu:xenial",
        command = "awk '{s+=$1} END {print s}' /input > /output")

    // Check if results matches with the one computed with the standard RDD API
    val toMatch = sc.textFile(getClass.getResource("dna/dna.txt").getPath)
      .map(_.count(c => c == 'g' || c == 'c').toString)
      .reduce {
        case (lineCount1, lineCount2) =>
          (lineCount1.toInt + lineCount2.toInt).toString
      }
    assert(res == toMatch + "\n")

  }

  test("GC count in DNA string, set volume files") {

    val rdd = sc.textFile(getClass.getResource("dna/dna.txt").getPath)

    val res = new MaRe(rdd)
      .setInputMountPoint("/input.dna")
      .setOutputMountPoint("/output.dna")
      .mapPartitions(
        imageName = "ubuntu:xenial",
        command = "grep -o '[gc]' /input.dna | wc -l > /output.dna")
      .reducePartitions(
        imageName = "ubuntu:xenial",
        command = "awk '{s+=$1} END {print s}' /input.dna > /output.dna")

    // Check if results matches with the one computed with the standard RDD API
    val toMatch = sc.textFile(getClass.getResource("dna/dna.txt").getPath)
      .map(_.count(c => c == 'g' || c == 'c').toString)
      .reduce {
        case (lineCount1, lineCount2) =>
          (lineCount1.toInt + lineCount2.toInt).toString
      }
    assert(res == toMatch + "\n")

  }
  
  // This is inefficient for this use case, but it is important to test
  test("GC count in DNA string, record by record") {
    
    val rdd = sc.textFile(getClass.getResource("dna/dna.txt").getPath)

    val res = new MaRe(rdd)
      .setReduceInputMountPoint1("/input1.txt")
      .setReduceInputMountPoint2("/input2.txt")
      .map(
        imageName = "ubuntu:xenial",
        command = "grep -o '[gc]' /input | wc -l > /output")
      .reduce(
        imageName = "ubuntu:xenial",
        command = "cat /input1.txt /input2.txt | awk '{s+=$1} END {print s}' > /output")

    // Check if results matches with the one computed with the standard RDD API
    val toMatch = sc.textFile(getClass.getResource("dna/dna.txt").getPath)
      .map(_.count(c => c == 'g' || c == 'c').toString)
      .reduce {
        case (lineCount1, lineCount2) =>
          (lineCount1.toInt + lineCount2.toInt).toString
      }
    assert(res == toMatch)

  }

}
