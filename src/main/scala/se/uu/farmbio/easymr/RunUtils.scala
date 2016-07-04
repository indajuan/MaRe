package se.uu.farmbio.easymr

import java.io.File
import java.io.PrintWriter
import scala.collection.JavaConverters.seqAsJavaListConverter
import scala.io.Source
import com.google.common.io.Files
import sys.process._
import org.apache.spark.Logging
import scala.concurrent.Future
import scala.util.{ Failure, Success }
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext
import java.util.concurrent.Executors


class RunException(msg: String) extends Exception(msg)

object RunUtils extends Logging {
  
  val FIFO_READ_TIMEOUT = 1200
  
  implicit val ec = ExecutionContext.fromExecutor(
      Executors.newFixedThreadPool(100))

  def writeToFifo(fifo: File, toWrite: String) = {
    logInfo(s"writing to fifo: ${fifo.getAbsolutePath}")
    Future {
      new PrintWriter(fifo) {
        write(toWrite)
        close
      }
    } onFailure {
      case (e) => e.printStackTrace
    }
  }

  def readFromFifo(fifo: File, timeoutSec: Int) = {
    logInfo(s"reading output from fifo: ${fifo.getAbsolutePath}")
    val future = Future {
      Source.fromFile(fifo).mkString
    } 
    Await.result(future, timeoutSec seconds)
  }

  def dockerRun(
    cmd: String,
    imageName: String,
    dockerOpts: String) = {
    command(s"docker run $dockerOpts $imageName sh -c ".split(" ") ++ Seq(cmd))
  }

  def mkfifo(name: String) = {
    val tmpDir = Files.createTempDir
    tmpDir.deleteOnExit
    val fifoPath = tmpDir.getAbsolutePath + s"/$name"
    val future = command(Seq("mkfifo", fifoPath), asynch = false)
    val fifo = new File(fifoPath)
    fifo.deleteOnExit
    fifo
  }

  def command(cmd: Seq[String], asynch: Boolean = true) = {
    logInfo(s"executing command: ${cmd.mkString(" ")}")
    val future = Future {
      cmd ! ProcessLogger(
        (o: String) => logInfo(o),
        (e: String) => logInfo(e))
    }
    future onComplete {
      case Success(exitCode) => {
        if (exitCode != 0) {
          throw new RunException(s"${cmd.mkString(" ")} exited with non-zero exit code: $exitCode")
        } else {
          logInfo(s"successfully executed command: ${cmd.mkString(" ")}")
        }
      }
      case Failure(e) => e.printStackTrace
    }
    if (!asynch) {
      Await.ready(future, Duration.Inf)
    }
  }

}