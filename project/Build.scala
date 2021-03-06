import com.ebiznext.sbt.plugins.CxfWsdl2JavaPlugin
import com.ebiznext.sbt.plugins.CxfWsdl2JavaPlugin.cxf._
import io.gatling.sbt.GatlingPlugin
import sbt.Keys._
import sbt._
import sbtprotobuf.{ProtobufPlugin => PB}
import sbtscalaxb.Plugin.ScalaxbKeys._
import sbtscalaxb.Plugin.scalaxbSettings

object Build extends sbt.Build {

  def subModule(id: String): Project = Project(id = id, base = file(s"coinffeine-$id"))

  lazy val root = Project(id = "coinffeine", base = file(".")).aggregate(
    alarms,
    common,
    commonAkka,
    commonTest,
    gui,
    headless,
    model,
    overlay,
    peer,
    protocol,
    tools
  )

  lazy val peer = (subModule("peer")
    settings(scalaxbSettings: _*)
    settings(
      sourceGenerators in Compile <+= scalaxb in Compile,
      packageName in (Compile, scalaxb) := "coinffeine.peer.payment.okpay.generated",
      dispatchVersion in (Compile, scalaxb) := Dependencies.Versions.dispatch,
      async in (Compile, scalaxb) := true
    )
    dependsOn(
      alarms,
      commonAkka % "compile->compile;test->test",
      commonTest % "test->compile",
      model % "compile->compile;test->test",
      protocol % "compile->compile;test->test"
    )
  )

  lazy val protocol = (subModule("protocol")
    settings(PB.protobufSettings: _*)
    dependsOn(
      alarms,
      common,
      commonAkka % "compile->compile;test->test",
      commonTest % "test->compile",
      model % "compile->compile;test->test",
      overlay % "compile->compile;test->test"
    )
  )

  lazy val overlay = (subModule("overlay")
    settings(PB.protobufSettings: _*)
    dependsOn(common, commonAkka % "compile->compile;test->test", commonTest % "test->compile")
  )

  lazy val model = (subModule("model")
    dependsOn(commonTest % "test->compile")
  )

  lazy val common = subModule("common").dependsOn(commonTest)

  lazy val commonAkka = subModule("common-akka")
    .dependsOn(common, commonTest % "compile->compile;test->test")

  lazy val commonTest = subModule("common-test").settings(PB.protobufSettings: _*)

  lazy val headless = (subModule("headless")
    settings(Assembly.settings("coinffeine.headless.Main"): _*)
    dependsOn(peer % "compile->compile;test->test", commonTest)
  )

  lazy val gui = (subModule("gui")
    configs IntegrationTest
    settings(Defaults.itSettings: _*)
    dependsOn(peer % "compile->compile;test,it->test", commonTest)
  )

  lazy val benchmark = (subModule("benchmark")
    dependsOn(
      commonAkka % "compile->compile;test->test",
      commonTest % "test->compile",
      peer % "compile->compile;test->test"
    )
    enablePlugins GatlingPlugin
  )

  lazy val tools = subModule("tools").dependsOn(
    commonTest,
    model % "compile->compile;test->test"
  )

  lazy val alarms = subModule("alarms").dependsOn(
    commonTest % "test->compile",
    commonAkka % "compile->compile;test->test"
  )
}
