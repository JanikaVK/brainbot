export type DailyAnswers = {
  energy: number;
  sleepQuality: number;
  workload: number;
  concentration: number;
  stress: number;
  socialConnection: number;
  studyHours: number;
  breakFrequency: number;
};

export type Suggestion = { title: string; detail: string };

/** Practical, non-clinical prompts based only on the student's current check-in. */
export function suggestionsFor(answers: DailyAnswers): Suggestion[] {
  const suggestions: Suggestion[] = [];
  if (answers.workload >= 4) {
    suggestions.push({ title: 'Make the next task smaller', detail: 'Choose one priority for your next study block and leave the rest for later.' });
  }
  if (answers.sleepQuality <= 2) {
    suggestions.push({ title: 'Keep today manageable', detail: 'If you can, choose a lighter first task and give yourself a little room to rest.' });
  }
  if ((answers.energy <= 2 || answers.concentration <= 2) && suggestions.length < 3) {
    suggestions.push({ title: 'Try a short focus block', detail: 'Work on one small step for 15–20 minutes, then pause and check what you need next.' });
  }
  if (answers.stress >= 4 && suggestions.length < 3) {
    suggestions.push({ title: 'Pause before the next task', detail: 'Take a few slow breaths or step away briefly before deciding what to do next.' });
  }
  if (answers.studyHours >= 2 && answers.breakFrequency < 1 && suggestions.length < 3) {
    suggestions.push({ title: 'Take a real break', detail: 'Step away from your study space for a few minutes before your next block.' });
  }
  if (answers.socialConnection <= 2 && suggestions.length < 3) {
    suggestions.push({ title: 'Reach out to someone you trust', detail: 'A quick message or short conversation can be a useful way to feel connected.' });
  }
  if (suggestions.length === 0) {
    suggestions.push({ title: 'Choose one manageable next step', detail: 'Pick something you can finish in one sitting, then decide when you’re done for now.' });
  }
  return suggestions.slice(0, 3);
}
