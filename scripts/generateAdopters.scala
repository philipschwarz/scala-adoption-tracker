#!/usr/bin/env scala-cli
//> using scala 3.5.1
//> using dep org.yaml:snakeyaml:2.2

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path, Paths}
import java.time.LocalDate
import scala.jdk.CollectionConverters.*

enum AdoptionStatus(val label: String):
  case NotPlanned extends AdoptionStatus("Not planned")
  case Planned extends AdoptionStatus("Planned")
  case Partial extends AdoptionStatus("Partial")
  case Full extends AdoptionStatus("Full")

object AdoptionStatus:
  def fromString(raw: String, fileName: String): AdoptionStatus =
    raw.trim.toLowerCase match
      case "not planned" => NotPlanned
      case "planned" => Planned
      case "partial" => Partial
      case "full" => Full
      case other =>
        val allowed = AdoptionStatus.values.map(_.label.toLowerCase).mkString(", ")
        throw new IllegalArgumentException(
          s"Invalid scala 3 adoption status '$other' in $fileName. Allowed: $allowed"
        )

enum Category(val label: String):
  case ProductCompany extends Category("product company")
  case OssProject extends Category("OSS project")
  case ConsultingCompany extends Category("consulting company")

object Category:
  def fromString(raw: String, fileName: String): Category =
    raw.trim.toLowerCase match
      case "product company" => ProductCompany
      case "oss project" => OssProject
      case "consulting company" => ConsultingCompany
      case other =>
        val allowed = Category.values.map(_.label).mkString(", ")
        throw new IllegalArgumentException(
          s"Invalid category '$other' in $fileName. Allowed: $allowed"
        )

final case class Adopter(
    name: String,
    logoUrl: String,
    website: String,
    usage: String,
    scala3AdoptionStatus: AdoptionStatus,
    category: Category,
    size: Int,
    sources: List[String]
)

object GenerateAdopters:
  private val yaml = new org.yaml.snakeyaml.Yaml()
  private val repoRoot: Path = Paths.get(Option(System.getProperty("repo.root")).getOrElse(".")).toAbsolutePath
  private val adoptersDir = repoRoot.resolve("adopters")
  private val outputFile = repoRoot.resolve("src").resolve("pages").resolve("index.md")
  def main(args: Array[String]): Unit =
    if !Files.isDirectory(adoptersDir) then
      System.err.println(s"No adopters found at ${adoptersDir.toAbsolutePath}")
      sys.exit(1)

    val adopters = Files.list(adoptersDir).iterator().asScala
      .filter(_.getFileName.toString.endsWith(".yaml"))
      .toList
      .sorted
      .map(loadAdopter)
      .sortBy(a => (-a.size, a.name.toLowerCase))

    if adopters.isEmpty then
      System.err.println("No adopter entries found in adopters directory.")
      sys.exit(1)

    val markdown = renderMarkdown(adopters)
    Files.createDirectories(outputFile.getParent)
    Files.writeString(outputFile, markdown, StandardCharsets.UTF_8)
    println(s"Wrote ${adopters.size} adopters to ${repoRoot.relativize(outputFile)}")

  private def loadAdopter(path: Path): Adopter =
    val data = yaml.load[java.util.Map[String, Any | Null]](Files.readString(path))
    val map = Option(data).map(_.asScala.toMap).getOrElse(Map.empty)
    def readOptionalString(key: String): Option[String] =
      map.get(key) match
        case Some(value: String) if value.trim.nonEmpty => Some(value.trim)
        case Some(value) if value != null =>
          val text = value.toString.trim
          Option.when(text.nonEmpty)(text)
        case _ => None
    def readRequired(key: String): String =
      readOptionalString(key).getOrElse {
        throw new IllegalArgumentException(s"Missing required key '$key' in ${path.getFileName}")
      }
    def readCategory(key: String): Category =
      Category.fromString(readRequired(key), path.getFileName.toString)
    def readStatusFlexible(): AdoptionStatus =
      val sourceKey =
        List("adoptionStatus", "scala3adoptionStatus").flatMap(readOptionalString).headOption.getOrElse {
          throw new IllegalArgumentException(
            s"Missing required key 'adoptionStatus' (or previous 'scala3adoptionStatus') in ${path.getFileName}"
          )
        }
      AdoptionStatus.fromString(sourceKey, path.getFileName.toString)
    def readInt(key: String): Int =
      map.get(key) match
        case Some(n: java.lang.Number) => n.intValue()
        case Some(s: String) if s.trim.nonEmpty => s.trim.toIntOption.getOrElse(throw new IllegalArgumentException(s"Key '$key' in ${path.getFileName} must be a number"))
        case Some(other) => throw new IllegalArgumentException(s"Key '$key' in ${path.getFileName} must be a number, got ${other.getClass.getSimpleName}")
        case None => throw new IllegalArgumentException(s"Missing required key '$key' in ${path.getFileName}")
    def readList(key: String): List[String] =
      map.get(key) match
        case Some(list: java.util.List[?]) =>
          list.asScala.collect { case s: String if s.trim.nonEmpty => s.trim }.toList
        case Some(item: String) if item.trim.nonEmpty => List(item.trim)
        case None => Nil
        case _ => throw new IllegalArgumentException(s"Key '$key' in ${path.getFileName} must be a list of strings")

    Adopter(
      name = readRequired("name"),
      logoUrl = readRequired("logoUrl"),
      website = readRequired("website"),
      usage = readRequired("usage"),
      scala3AdoptionStatus = readStatusFlexible(),
      category = readCategory("category"),
      size = readInt("size"),
      sources = readList("sources")
    )

  private def renderMarkdown(adopters: List[Adopter]): String =
    val statusStats = adopters.groupMapReduce(_.scala3AdoptionStatus)(_ => 1)(_ + _)
    val summaryLines = AdoptionStatus.values.toList.map { status =>
      val count = statusStats.getOrElse(status, 0)
      s"- **${status.label}**: $count"
    }

    val builder = new StringBuilder
    builder.append("---\n")
    builder.append("title: Scala Adoption Tracker\n")
    builder.append("---\n\n")
    builder.append("# Scala Adoption Tracker\n\n")
    builder.append(
      "This page lists companies and projects that publicly share information about their Scala usage. " +
        "Entries are crowdsourced via YAML files in the `/adopters` directory and rendered using the Scala generator script.\n\n"
    )
    builder.append(s"Last updated ${LocalDate.now}.\n\n")
    builder.append("## Scala 3 adoption snapshot\n\n")
    summaryLines.foreach(line => builder.append(line).append('\n'))
    builder.append("\n## Adopters\n\n")

    adopters.foreach { adopter =>
      builder.append(s"### ${adopter.name}\n\n")
      builder.append(s"![${adopter.name} logo](${adopter.logoUrl})\n\n")
      builder.append(s"- **Website:** [${adopter.website}](${adopter.website})\n")
      builder.append(s"- **Category:** ${adopter.category.label}\n")
      builder.append(s"- **Company size:** ${adopter.size}\n")
      builder.append(s"- **Scala 3 adoption status:** ${adopter.scala3AdoptionStatus.label}\n")
      builder.append(s"- **Usage:** ${adopter.usage}\n")
      if adopter.sources.nonEmpty then
        builder.append("- **Sources:**\n")
        adopter.sources.foreach { source =>
          val bullet = if source.startsWith("http") then s"  - [${source}](${source})" else s"  - ${source}"
          builder.append(bullet).append('\n')
        }
      builder.append('\n')
    }

    builder.toString()
