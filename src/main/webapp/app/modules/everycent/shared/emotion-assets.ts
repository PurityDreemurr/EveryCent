const emotionImageBasePath = '/content/images/emotions/';

const emotionImageEntries = [
  ['happy', ['Happy', '开心', '高兴', '快乐', '愉快']],
  ['calm', ['Calm', '平静', '冷静', '安定']],
  ['angry', ['Angry', '生气', '愤怒', '恼火', 'Impulsive', '冲动']],
  ['fearful', ['Fearful', '害怕', '恐惧', '担心', 'Anxious', '焦虑']],
  ['sad', ['Sad', '难过', '伤心', 'Regret', '后悔']],
  ['tired', ['Tired', '疲惫', '疲劳', '累', 'Stressed', '压力']],
  ['pleased', ['Pleased', '满意', '愉悦', 'None', '无明显情绪']],
  ['relieved', ['Relieved', '释然', '放松', '松了一口气']],
  ['surprised', ['Surprised', '惊讶', '意外', '震惊']],
  ['disgusted', ['Disgusted', '厌恶', '反感', '嫌弃']],
  ['depressed', ['Depressed', '沮丧', '低落', '郁闷']],
  ['grieved', ['Grieved', '悲伤', '哀伤', '悲痛']],
] as const;

export const emotionImageMap: Record<string, string> = Object.fromEntries(
  emotionImageEntries.flatMap(([fileName, aliases]) => [
    [fileName, `${emotionImageBasePath}${fileName}.png`],
    [fileName.toUpperCase(), `${emotionImageBasePath}${fileName}.png`],
    ...aliases.map(alias => [alias, `${emotionImageBasePath}${fileName}.png`]),
  ]),
);

export const getEmotionImage = (name?: string | number) => {
  if (name === undefined || name === null) {
    return undefined;
  }

  const key = String(name).trim();
  return emotionImageMap[key] || emotionImageMap[key.toLowerCase()] || emotionImageMap[key.toUpperCase()];
};
