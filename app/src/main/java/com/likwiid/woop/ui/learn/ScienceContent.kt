// SPDX-License-Identifier: GPL-3.0-or-later

package com.likwiid.woop.ui.learn

import com.likwiid.woop.ui.theme.WoopStep

enum class Strength(val label: String) {
    STRONG("Strong"),
    GOOD("Good"),
    EARLY("Early"),
    HYPOTHESIS("Hypothesis"),
    PRACTITIONER("Practitioner"),
}

data class Evidence(
    val title: String,
    val takeaway: String,
    val detail: String,
    val inApp: String,
    val sources: List<String>,
    val strength: Strength,
)

data class MethodStep(val step: WoopStep, val text: String)

val methodSteps = listOf(
    MethodStep(WoopStep.WISH, "Something that matters to you, challenging but doable."),
    MethodStep(WoopStep.OUTCOME, "The single best result. Picture it, and how it will feel."),
    MethodStep(WoopStep.OBSTACLE, "The main thing in you that could get in the way. Picture that too."),
    MethodStep(WoopStep.PLAN, "If [obstacle], then I will [action]."),
)

val strengthLegend = listOf(
    Strength.STRONG to "Meta-analyses of many studies",
    Strength.GOOD to "Several controlled experiments",
    Strength.EARLY to "Few or small studies, or one group",
    Strength.HYPOTHESIS to "An idea, not yet tested directly",
    Strength.PRACTITIONER to "Expert advice from practice",
)

val evidence = listOf(
    Evidence(
        title = "If-then plans make action automatic",
        takeaway = "Deciding \"when X happens, I'll do Y\" ahead of time reliably helps people follow through.",
        detail = "An implementation intention links a specific situation to a specific response. When the situation shows up, the response starts with little conscious effort, so it needs less willpower in the moment. A meta-analysis of 94 tests found a medium-to-large effect on reaching goals (d ≈ 0.65).",
        inApp = "Every obstacle gets an \"If…, then I will…\" plan. Your first if-then sits on Today and inside every check-in notification.",
        sources = listOf(
            "Gollwitzer, P. M. (1999). Implementation intentions. American Psychologist, 54, 493–503.",
            "Gollwitzer, P. M., & Sheeran, P. (2006). Implementation intentions and goal achievement: A meta-analysis. Advances in Experimental Social Psychology, 38, 69–119.",
        ),
        strength = Strength.STRONG,
    ),
    Evidence(
        title = "WOOP works, with a modest effect",
        takeaway = "Across 21 studies and about 16,000 people, WOOP helped people reach goals. Interactive versions helped most.",
        detail = "A 2021 meta-analysis found a small-to-medium overall effect (g = 0.34). Guided, interactive delivery did better than paper worksheets (g = 0.47 vs 0.28). The authors note possible publication bias, so the true effect may be a little smaller.",
        inApp = "The app guides you one step at a time instead of handing you a blank form.",
        sources = listOf(
            "Wang, G., Wang, Y., & Gai, X. (2021). A meta-analysis of the effects of mental contrasting with implementation intentions on goal attainment. Frontiers in Psychology, 12, 565202.",
        ),
        strength = Strength.GOOD,
    ),
    Evidence(
        title = "Dreaming alone drains your drive",
        takeaway = "Only imagining the ideal future can lower your energy and predict doing less.",
        detail = "In experiments, people who indulged in positive fantasies about the future showed lower energy, measured by systolic blood pressure, and later achieved less than people who also considered what stood in the way.",
        inApp = "Picturing the outcome is never the last step. The obstacle always comes right after it.",
        sources = listOf(
            "Kappes, H. B., & Oettingen, G. (2011). Positive fantasies about idealized futures sap energy. Journal of Experimental Social Psychology, 47, 719–729.",
            "Oettingen, G., & Mayer, D. (2002). The motivating function of thinking about the future. Journal of Personality and Social Psychology, 83, 1198–1212.",
        ),
        strength = Strength.GOOD,
    ),
    Evidence(
        title = "Order matters: dream first, obstacle second",
        takeaway = "Contrasting future then reality makes your commitment match your real chances.",
        detail = "When people imagine the wished-for future first and then the present obstacle, they commit strongly to goals they're likely to reach, and ease off ones they aren't. Thinking only about the dream, only about the obstacles, or in reverse order doesn't produce this.",
        inApp = "Steps always run W → O → O → P. The \"How likely?\" question offers to shrink a wish that feels out of reach.",
        sources = listOf(
            "Oettingen, G., Pak, H., & Schnetter, K. (2001). Self-regulation of goal setting. Journal of Personality and Social Psychology, 80, 736–753.",
            "Oettingen, G., et al. (2009). Mental contrasting and goal commitment: The mediating role of energization. Personality and Social Psychology Bulletin, 35, 608–622.",
        ),
        strength = Strength.GOOD,
    ),
    Evidence(
        title = "Obstacles become cues for action",
        takeaway = "Mental contrasting builds a mental link between an obstacle and what you'll do about it.",
        detail = "After mental contrasting, people respond faster to their obstacle with the behaviour that overcomes it. In effect, the obstacle starts to remind you of the plan.",
        inApp = "Each obstacle gets its own plan, and the \"if\" starts out as your obstacle, in your words.",
        sources = listOf(
            "Kappes, A., Singmann, H., & Oettingen, G. (2012). Mental contrasting instigates goal pursuit by linking obstacles of reality with instrumental behavior. Journal of Experimental Social Psychology, 48, 811–818.",
        ),
        strength = Strength.GOOD,
    ),
    Evidence(
        title = "Re-reading keeps the cue ready",
        takeaway = "If-then plans work partly by keeping the cue \"top of mind\", so you notice the moment when it comes.",
        detail = "Studies of how implementation intentions work found two ingredients: the cue becomes easier to notice, and the cue–response link gets stronger. A short re-read is a simple way to keep both fresh.",
        inApp = "Check-ins are a one-minute re-read, and the reminder text repeats your if-then.",
        sources = listOf(
            "Webb, T. L., & Sheeran, P. (2007). How do implementation intentions promote goal attainment? Journal of Experimental Social Psychology, 43, 295–302.",
            "Webb, T. L., & Sheeran, P. (2008). Mechanisms of implementation intention effects. British Journal of Social Psychology, 47, 373–395.",
        ),
        strength = Strength.GOOD,
    ),
    Evidence(
        title = "Mental images carry more feeling",
        takeaway = "Picturing something stirs more emotion than thinking about it in words.",
        detail = "In experiments, imagining events produced stronger emotional reactions than processing the same events verbally. That's why WOOP asks you to picture the outcome and the obstacle, not just name them.",
        inApp = "Optional 30-second \"picture it\" pauses after the Outcome and the Obstacle.",
        sources = listOf(
            "Holmes, E. A., & Mathews, A. (2005). Mental imagery and emotion: A special relationship? Emotion, 5, 489–497.",
        ),
        strength = Strength.GOOD,
    ),
    Evidence(
        title = "Letting go is a healthy result",
        takeaway = "Disengaging from goals that have become unreachable is linked to better well-being.",
        detail = "People who could let go of unattainable goals, and re-engage with new ones, reported better well-being across several studies. Quitting the right wish is a skill, not a failure.",
        inApp = "\"Let it go\" sits next to \"Achieved\" as a valid outcome, with no guilt attached.",
        sources = listOf(
            "Wrosch, C., Scheier, M. F., Miller, G. E., Schulz, R., & Carver, C. S. (2003). Adaptive self-regulation of unattainable goals. Personality and Social Psychology Bulletin, 29, 1494–1508.",
        ),
        strength = Strength.GOOD,
    ),
    Evidence(
        title = "Helps with ADHD-type challenges",
        takeaway = "In children with ADHD symptoms, WOOP and if-then plans improved self-control.",
        detail = "MCII improved self-regulation in children at risk for ADHD, and if-then plans improved response inhibition. Studies are small and mostly in children; adult data is still limited.",
        inApp = "One question per screen, autosave, voice input, examples on tap, and no streaks to break.",
        sources = listOf(
            "Gawrilow, C., Morgenroth, K., Schultz, R., Oettingen, G., & Gollwitzer, P. M. (2013). Mental contrasting with implementation intentions enhances self-regulation of goal pursuit in schoolchildren at risk for ADHD. Motivation and Emotion, 37, 134–145.",
            "Gawrilow, C., & Gollwitzer, P. M. (2008). Implementation intentions facilitate response inhibition in children with ADHD. Cognitive Therapy and Research, 32, 261–280.",
        ),
        strength = Strength.EARLY,
    ),
    Evidence(
        title = "Keeping goals private can help",
        takeaway = "Announcing an identity goal can feel like progress, and lower the effort you put in.",
        detail = "When others took notice of people's identity-related intentions (such as a professional identity), those people later worked less on them. The finding is specific to identity goals, and later replications are mixed.",
        inApp = "No sharing, no accounts, no internet access. Your WOOPs never leave the phone unless you export them.",
        sources = listOf(
            "Gollwitzer, P. M., Sheeran, P., Michalski, V., & Seifert, A. E. (2009). When intentions go public. Psychological Science, 20, 612–618.",
        ),
        strength = Strength.EARLY,
    ),
    Evidence(
        title = "A cyclic sigh calms you down",
        takeaway = "Two inhales through the nose, then one long exhale, can lower arousal and lift mood.",
        detail = "In a month-long randomized study, five minutes a day of cyclic sighing improved mood and lowered breathing rate more than mindfulness meditation. The effect of a single sigh is less studied.",
        inApp = "A one-line tip when you picture your obstacle, since that can feel stressful.",
        sources = listOf(
            "Balban, M. Y., et al. (2023). Brief structured respiration practices enhance mood and reduce physiological arousal. Cell Reports Medicine, 4, 100895.",
        ),
        strength = Strength.EARLY,
    ),
    Evidence(
        title = "Write it once, re-read when motivation dips",
        takeaway = "Huberman suggests a slower first write-up, then quick re-reads and edits over time.",
        detail = "In Protocols, Huberman recommends writing out your answers the first time (around ten minutes), then coming back to re-read and refine them whenever your drive drops. He describes the goal as creating a sense of urgency.",
        inApp = "Creating a WOOP is the slow pass; a check-in is the quick re-read and tune.",
        sources = listOf("Huberman, A. Protocols: An Operating Manual for the Human Body. Learning Protocol 2, on mental contrasting."),
        strength = Strength.PRACTITIONER,
    ),
    Evidence(
        title = "The \"tenacity\" brain region",
        takeaway = "Huberman speculates WOOP may engage the anterior mid-cingulate cortex (aMCC). This hasn't been tested.",
        detail = "The aMCC is linked to effort and persistence. Huberman suggests that practices like WOOP could engage it, and says himself that there is no direct data yet. Treat this as an interesting idea, not a reason to use the method.",
        inApp = "Not used to justify any feature. It's shown here so you can see what is and isn't established.",
        sources = listOf(
            "Touroutoglou, A., Andreano, J., Dickerson, B. C., & Barrett, L. F. (2020). The tenacious brain: How the anterior mid-cingulate contributes to achieving goals. Cortex, 123, 12–29.",
        ),
        strength = Strength.HYPOTHESIS,
    ),
)
