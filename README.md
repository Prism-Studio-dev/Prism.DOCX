<div align="center">

<img src="src/main/composeResources/drawable/prism_docx_app_icon.svg" alt="Иконка Prism.DOCX" width="88" height="88">

<h1>Prism.DOCX Unstable</h1>

<p>Настольное приложение для просмотра и редактирования метаданных документов Microsoft Word (.docx).</p>

<p>
  <img src="https://img.shields.io/badge/Kotlin-2.1.0-7F52FF?style=flat-square&amp;logo=kotlin&amp;logoColor=white" alt="Kotlin 2.1.0">
  <img src="https://img.shields.io/badge/Compose%20Desktop-1.7.3-4285F4?style=flat-square" alt="Compose Desktop 1.7.3">
  <img src="https://img.shields.io/badge/Platform-Windows-0078D4?style=flat-square&amp;logo=windows&amp;logoColor=white" alt="Windows">
  <a href="LICENSE"><img src="https://img.shields.io/badge/License-MIT-2E7D32?style=flat-square" alt="MIT License"></a>
  <img src="https://img.shields.io/badge/Version-v1.1.1-424242?style=flat-square" alt="Version v1.1.1">
</p>

<p>
  <a href="#о-проекте">О проекте</a> ·
  <a href="#возможности">Возможности</a> ·
  <a href="#установка">Установка</a> ·
  <a href="#использование">Использование</a> ·
  <a href="#сборка-из-исходников">Сборка</a> ·
  <a href="#технологии">Технологии</a> ·
  <a href="#лицензия">Лицензия</a>
</p>

</div>

## Prism.DOCX Unstable

Эта unstable-ветка экспериментальная и не является стабильным релизом.
Она содержит Windows Rust Core (Kotlin → JNA → Rust), который собирается в Release
и поставляется с EXE/MSI `PrismDOCXUnstable`. Для сборки этой ветки кроме JDK 21
нужны Cargo и Rust MSVC toolchain. Установленному приложению они не нужны.

Production XML validation остаётся Kotlin: у экспериментального Rust validator
есть подтверждённые semantic differences. Если native library недоступна или
несовместима, приложение продолжает работать с Kotlin и пишет diagnostic в console.
Development benchmark показал разные результаты в зависимости от размера XML;
общее преимущество Rust над Kotlin не заявляется.
Подробности, Debug/Release tasks, integration tests и benchmark —
[Rust Core README](native/prism-docx-core/README.md).

## О проекте

Prism.DOCX помогает управлять свойствами Word-документа, не затрагивая его текст. Исходный файл остаётся без изменений: результат сохраняется в отдельную копию.

Стандартные и пользовательские свойства можно редактировать в форме. Для редких и составных свойств есть XML-редактор: в нём можно просматривать и менять содержимое разделов метаданных <code>core</code>, <code>app</code> и <code>custom</code>.

## Возможности

- Просмотр и редактирование 39 стандартных свойств, поиск по ним.
- Добавление, изменение и удаление пользовательских свойств: текст, целые и дробные числа, логические значения и даты.
- Просмотр и редактирование редких и составных значений через XML-редактор.
- Диалоги открытия DOCX и сохранения копии соответствуют выбранной теме; документ также можно перетащить в окно приложения.
- Встроенные темы Prism, Светлая, Тёмная, Graphite, Violet, Emerald и Nord; выбор в меню «Настройки» сохраняется между запусками.
- Проверка распространённых типов данных и предупреждение о несохранённых изменениях.
- Сохранение в отдельную копию с подтверждением замены уже существующего результата.

## Установка

1. Откройте [GitHub Releases](../../releases).
2. Скачайте один из установщиков для Windows:
   - <code>PrismDOCX-1.1.1.exe</code>
   - <code>PrismDOCX-1.1.1.msi</code>
3. Запустите установщик и завершите установку приложения.

## Использование

1. Откройте DOCX через диалог выбора файла или перетащите один файл в окно приложения.
2. Просмотрите свойства документа и при необходимости найдите нужное поле поиском.
3. Измените стандартные или пользовательские свойства; для редких и составных значений откройте XML-редактор.
4. Сохраните результат в отдельную копию. Исходный документ не перезаписывается.

## Сборка из исходников

Для сборки требуется JDK 21. Проект поставляется с Gradle Wrapper 8.7, который загрузит Gradle при первом запуске. Команды выполняются в PowerShell из корня проекта:

~~~powershell
.\gradlew.bat run
.\gradlew.bat test
.\gradlew.bat build
.\gradlew.bat packageExe packageMsi
~~~

Установщики появятся в <code>build/compose/binaries/main/exe</code> и <code>build/compose/binaries/main/msi</code>.

## Технологии

- Kotlin 2.1.0
- Compose Multiplatform Desktop 1.7.3 и Material 3
- JDK 21 и Gradle Wrapper 8.7
- OOXML DOCX: чтение и запись ZIP-пакета и XML-частей средствами Java

## Ограничения

Prism.DOCX редактирует три части свойств DOCX (<code>core</code>, <code>app</code> и <code>custom</code>), но не меняет текст документа, файловые даты Windows, комментарии, исправления и произвольные части <code>customXml</code>. XML-редактор проверяет синтаксис, корневой элемент и распространённые типы, но не выполняет полную проверку по схеме OOXML. Microsoft Word может пересчитать статистику при следующем сохранении; изменение XML <code>DigSig</code> само по себе не создаёт действительную цифровую подпись. Поэтому приложение не является средством полного обезличивания документов.

## Лицензия

Prism.DOCX распространяется по лицензии [MIT License](LICENSE).
