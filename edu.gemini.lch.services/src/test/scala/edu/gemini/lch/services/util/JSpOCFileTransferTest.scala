package edu.gemini.lch.services.util

import java.io.File
import java.net.URLDecoder
import javax.annotation.Resource

import edu.gemini.lch.model.Site
import edu.gemini.lch.services.{LaserNightService, JSpOCCommunicatorService}
import org.junit.{Ignore, Assert, Test}
import org.junit.runner.RunWith
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner

import scala.collection.immutable.HashMap
import edu.gemini.lch.data.fixture.DatabaseFixture
import org.springframework.util.FileCopyUtils

/**
 * This is a test environment that allows to test upload/download of files to/from space-track manually.
 * Run testUploads() to upload some files and then testDownloads() to download those files again.
 * Make sure to use the test folders on space-track for uploads. Downloads go into /tmp
 * This is really only meant for debugging in case space-track changes things and we need to figure out
 * what broke. Note that with the current configuration the pam and prm id values in the services implementation
 * JSpOCCommunicatorServiceImpl.scala are not set properly when running this test case manually, set them
 * to 339 (test folder for Gemini North) directly and comment out the @Value annotation to work around this.
 */
@RunWith(classOf[SpringJUnit4ClassRunner])
@ContextConfiguration(locations = Array("/spring-services-test-context.xml"))
class JSpOCFileTransferTest extends DatabaseFixture {

  import Assert._

  @Resource
  var communicator: JSpOCCommunicatorService = null

  @Resource
  var laserNightService: LaserNightService = null

  // Base path for files for downloading / uploading.
  private val BasePath = new File("/tmp/").getPath

  // List of files to download by site.
  private val DownloadsBySite = HashMap[Site, List[String]](
    Site.NORTH -> List("Test-GN-Please-Ignore.txt"),
    Site.SOUTH -> List("Test-GS-Please-Ignore.txt")
  )

  // List of files to upload by site.
  private val UploadsBySite = HashMap[Site, List[String]](
    Site.NORTH -> List("/testfiles/Test-GN-Please-Ignore.txt"),
    Site.SOUTH -> List("/testfiles/Test-GS-Please-Ignore.txt")
  )

  private def testDownloadFileTo(site: Site, remoteFilename: String, localFilename: String): Unit = {
    val connectStatus = communicator.connect(site)
    assertTrue(connectStatus.isSuccess)

    val downloadStatus = communicator.downloadFile(remoteFilename, new File(localFilename))
    assertTrue(downloadStatus.isSuccess)
  }

  // ======== FOR MANUAL TESTING ONLY ============
  // Note that we have two test folders ("Test Gemini North" and "Test Gemini South") available at www.space-track.org.
  // Be careful not to upload test files to the production folders ("GEMINI NORTH/PRM", "GEMINI SOUTH/PRM").

  @Ignore
  @Test
  def testDownloadSouth(): Unit = {
    val connectStatus = communicator.connect(Site.SOUTH)
    assertTrue(connectStatus.isSuccess)

    val downloadStatus = communicator.downloadNewPAMFiles(Site.SOUTH, 17321)
    assertTrue(downloadStatus.isSuccess)
  }


  @Ignore
  @Test
  def testGetFileIdsSouth(): Unit = {
    val connectStatus = communicator.connect(Site.SOUTH)
    assertTrue(connectStatus.isSuccess)

    val downloadStatus = communicator.newFileIDs(220, 17321)
    assertTrue(downloadStatus.isSuccess)
  }

  @Ignore
  @Test
  def testGetFileIdsNorth(): Unit = {
    val connectStatus = communicator.connect(Site.NORTH)
    assertTrue(connectStatus.isSuccess)

    val downloadStatus = communicator.newFileIDs(191, 0)
    assertTrue(downloadStatus.isSuccess)
  }

  @Ignore
  @Test
  def testDownloads(): Unit = {
    for {
      site           <- Site.values()
      remoteFilename <- DownloadsBySite(site)
    } testDownloadFileTo(site, remoteFilename, BasePath + File.separator + remoteFilename)
  }

  private def testUploadFile(site: Site, localResource: String): Unit = {
    val connectStatus = communicator.connect(site)
    assertTrue(connectStatus.isSuccess)

    val resourceName = URLDecoder.decode(getClass.getResource(localResource).getPath, "UTF-8")
    val file = new File(resourceName)
    val bytes = FileCopyUtils.copyToByteArray(file)
    val uploadStatus = communicator.uploadPRMFile(site, file.getName, bytes)
    assertTrue(uploadStatus.isSuccess)
  }

  @Ignore
  @Test
  def testUploads(): Unit = {
    for {
      site          <- Site.values()
      localResource <- UploadsBySite(site)
    } testUploadFile(site, localResource)
  }

}
