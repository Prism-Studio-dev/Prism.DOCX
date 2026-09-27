<div align="center">

<img src="src/main/composeResources/drawable/haruhi.png" alt="Prism.DOCX Haruhi Edition" width="96" height="96">

<h1>Prism.DOCX (MHS Edition)</h1>

<p>Форк настольного редактора метаданных Microsoft Word (.docx) с тематическим визуальным оформлением по мотивам аниме «Меланхолия Харухи Судзумии» (The Melancholy of Haruhi Suzumiya).</p>

<p>
  <img src="https://img.shields.io/badge/Fork-Prism.DOCX-blue?style=flat-square" alt="Fork of Prism.DOCX">
  <img src="https://img.shields.io/badge/Theme-Haruhi%20Suzumiya-E91E63?style=flat-square" alt="Haruhi Theme">
  <img src="https://img.shields.io/badge/Kotlin-2.1.0-7F52FF?style=flat-square&amp;logo=kotlin&amp;logoColor=white" alt="Kotlin 2.1.0">
  <img src="https://img.shields.io/badge/Compose%20Desktop-1.7.3-4285F4?style=flat-square" alt="Compose Desktop 1.7.3">
  <img src="https://img.shields.io/badge/Platform-Windows-0078D4?style=flat-square&amp;logo=windows&amp;logoColor=white" alt="Windows">
  <a href="LICENSE"><img src="https://img.shields.io/badge/License-MIT-2E7D32?style=flat-square" alt="MIT License"></a>
</p>

<p>
  <a href="#о-форке">О форке</a> ·
  <a href="#изменения">Изменения</a> ·
  <a href="#возможности">Возможности</a> ·
  <a href="#сборка-из-исходников">Сборка</a> ·
  <a href="#оригинальный-проект">Оригинал</a> ·
  <a href="#лицензия">Лицензия</a>
</p>

</div>

## О форке

Данный репозиторий представляет собой неофициальную кастомную редакцию [Prism.DOCX](https://github.com/Prism-Studio-dev/Prism.DOCX). Функционал работы со свойствами файлов DOCX сохранён в исходном виде, но весь интерфейс стилизован под атмосферу команды SOS и персонажа Харухи Судзумии.

## Возможности

- Просмотр и редактирование 39 стандартных свойств Word, быстрый поиск по полям.
- Добавление, изменение и удаление пользовательских свойств (текст, числа, булевы значения, даты).
- Встроенный XML-редактор для прямого редактирования разделов `core`, `app` и `custom`.
- Drag-and-drop открытие файлов DOCX прямо в окно приложения.
- Безопасное сохранение изменений в отдельную копию файла без изменения оригинального документа.
- Поддержка светлой и тёмной тем оформления.

## Сборка из исходников

Для сборки требуется JDK 21. Команды выполняются в терминале PowerShell из корневой директории проекта:

~~~powershell
# Запуск приложения
.\gradlew.bat run

# Сборка установочных пакетов для Windows
.\gradlew.bat packageExe packageMsi
~~~

Собранные установщики будут находиться в директориях `build/compose/binaries/main/exe` и `build/compose/binaries/main/msi`.

## Технологии

- Kotlin 2.1.0
- Compose Multiplatform Desktop 1.7.3 & Material 3
- Java 21 & Gradle Wrapper 8.7
- OOXML DOCX (ZipFileSystem, DOM XML)

## Оригинальный проект

Оригинальная кодовая база и разработка ядра редактора: [Prism-Studio-dev/Prism.DOCX](https://github.com/Prism-Studio-dev/Prism.DOCX).

## Лицензия

Как и оригинальный проект, форк распространяется под лицензией [MIT License](LICENSE).
