# Project Overview

This project has two goals:

1. **Automate a real-world drudge task** that’s currently handled manually at my workplace.
2. **Stress-test modern AI coding tools** by seeing if they can carry a small but realistic project end-to-end.

Most AI demos are toy examples—“make me a website” or “switch my theme to dark mode”—things we already have good tools for. I wanted to see how AI handles something closer to real software engineering.
What made this interesting—and hard for AI—is the nature of the problem: discrete matching of messy, uncontrolled data. There’s no clean dataset and no chance of outsourcing cleanup; it’s a reality we have to live with. This rules out machine learning as the primary approach. Instead, the challenge is about building rules-based logic that encodes human domain expertise—the kind of thing developers and analysts do all the time, but AI tends to stumble on.

A subgoal was to see how much I could get done **without paying**. I don’t mind paying if a tool is genuinely useful, but I didn’t want to spend money just to kick the tires.

Of course, the AI tools' capabilities and prices are very much in flux, so, as we move further and further away from mid-August 2025, take this with a bigger and bigger grain of salt.

---

## Step 1: Requirements with Claude Sonnet 4.0

I started with **Claude Sonnet 4.0** in interactive mode. Over several sessions, we hammered out:

* A **foundation prompt** for overall system design.
* A **detailed requirements document** for implementation.

This part worked smoothly—no surprise, since LLMs excel at text generation and structure. Both documents are published alongside this README.
- **[System Requirements](system-requirements.md):** Detailed business and functional requirements
- **[Foundation Prompt](foundation-prompt.md)** The initial AI prompt used for system analysis and design

Note that the **rules** in the "rules-based-logic" part were entirely provided my me, based on my domain expertise. I had spent quite some time debugging weird issues caused by duplicate customer records, and trying to come up with ways to avoid such situations. I knew what needed to be done, I had just not put it together in an end-to-end automated process. While it was comforting (for the project's sake) to see that the AI recognized the issues as I was describing them, I don't think there is a way for somebody unfamiliar with them to somehow elicit the rules/domain knowledge from the AI.

---

## Step 2: Code Generation with Claude Opus 4.1

For coding, I tried **Claude Opus 4.1**, often described as Anthropic’s best model for software. Unfortunately, it’s locked behind a paywall, so I subscribed to the \$20/month plan.

The experience was… mixed:

* **Monolithic code dump**: At first, Opus generated the entire project as a single giant Java file. Unsurprisingly, that didn’t work—its own final edits and refactorings failed, leaving me with corrupted code.
* **Separate files attempt**: Asking for one file per class was better, but still messy:

  * File names didn’t match class names.
  * Package structure was missing.
  * The generated `build.gradle` was incorrect (wrong dependency names).
* **Plan limits**: The \$20 plan cut me off repeatedly in the middle of generation, causing long breaks until the limits were reset. Resuming took forever.

Result: I had to manually rename files, create the proper folder structure, and fix the build config just to get it to load as a Gradle project in IntelliJ.

Bottom line: **not impressed**. For serious enterprise-style Java projects, Opus + \$20 plan isn’t viable. The next tier costs \$100+/month, but that would only have solved the annoying interruptions, not the quality issues.

---

## Step 3: Cleanup with Google Jules

I didn’t want to clean up all the Opus mess by hand, this would have defeated the purpose of testing AI tools on an end-to-end project.
In my initial plan, I would have tried Claude Code at this stage, but it can not be tried for free, not even with the \$20 plan, and, I strongly suspect, not even with the next, \$100+ plan, I would have needed to pay on top of that for usage. Plus, at best, it would have used behind the scenes the same Opus model that had already dissapointed in the previous step.

The situation seems very similar with GPT5.

Luckily, I stumbled upon **Google Jules**.

Caveat: Jules can’t work on your local filesystem—you have to push your code to GitHub. That’s why this repo exists.

My experience:

* Jules is **free**, but runs asynchronously. Sometimes it responded quickly, other times it went completely silent for hours until I “woke it up.” The promised “ping” notification when jobs finish never worked.
* When it *did* work, it was solid:

  * Cleaned up Opus’s broken output.
  * Helped restructure the project.
  * Generated unit tests.
  * Steps towards productizing: use HikariConfig and data source classname instead of jdbcUrl, robust text handling with encoding detection using `icu4j`, migrate to Java 17 and newer versions of the dependencies

Overall, Jules felt like working with a **“smart intern.”** Not always reliable, but capable of doing useful work. More than that, once it uderstood what needed to be done, it would do it faster than I could myself.

---

## Results

* **Time spent**: \~2 weekends

  * Weekend 1: requirements with Sonnet + messy code generation with Opus.
  * Weekend 2: fixes and unit tests with Jules.
* **Current state**: A working Java project that I can now test/debug with real data.
* **Success criteria**: The goal is for the automation to reach the accuracy of a “smart intern” doing the task manually. Too early to say if that bar is met—but Jules itself already *felt* like a smart intern during development.

---

* **I should mention that, while the business problem addressed by the project is general in nature, I have encountered it in the context of LoanIQ interfacing with outside data, so it is anchored on LoanIQ.
I have described, in general terms, which entity attributes useful for matching are stored in LoanIQ, but none of the "LoanIQ" queries generated by the AI are real, as the AIs did not have access to the actual LoanIQ data model**

---

## 
