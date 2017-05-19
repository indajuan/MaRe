package se.uu.it.easymr

import java.io.File
import java.io.PrintWriter
import java.util.UUID
import java.io.FileNotFoundException

private[easymr] object EasyFiles {
  
  // Set temporary directory
  private val tmpDir = if (System.getenv("TMPDIR") != null) {
    new File(System.getenv("TMPDIR"))
  } else {
    new File("/tmp")
  }
  if(!tmpDir.exists) {
    throw new FileNotFoundException(
        s"temporary directory ${tmpDir.getAbsolutePath} doesn't extist")
  }
  if(!tmpDir.isDirectory) {
    throw new FileNotFoundException(
        s"${tmpDir.getAbsolutePath} is not a directory")
  }
  
  private def newTmpFile = new File(tmpDir, "easymr_" + UUID.randomUUID.toString)
  
  def createTmpFile = {
    val file = EasyFiles.newTmpFile
    file.createNewFile
    file
  }
  
  def writeToTmpFile(it: Iterator[String]): File = {
    val file = EasyFiles.newTmpFile
    val pw = new PrintWriter(file)
    while(it.hasNext) {
      val line = it.next
      if(it.hasNext) {
        pw.println(line)
      } else {
        pw.write(line)
      }
    }
    pw.close
    file
  }
  
}