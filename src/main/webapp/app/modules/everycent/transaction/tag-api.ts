import axios from 'axios';

export interface TagOption {
  id: number;
  code?: string;
  name: string;
}

const behaviorNameMap: Record<string, string> = {
  FOOD: '餐饮',
  Food: '餐饮',
  TRANSPORT: '交通',
  Transport: '交通',
  SHOPPING: '购物',
  Shopping: '购物',
  ENTERTAINMENT: '娱乐',
  Entertainment: '娱乐',
  STUDY: '学习',
  Study: '学习',
  MEDICAL: '医疗',
  Medical: '医疗',
  SALARY: '工资',
  Salary: '工资',
  PART_TIME: '兼职',
  'Part time': '兼职',
  OTHER: '其他',
  Other: '其他',
};

const emotionNameMap: Record<string, string> = {
  HAPPY: '开心',
  Happy: '开心',
  CALM: '平静',
  Calm: '平静',
  IMPULSIVE: '冲动',
  Impulsive: '冲动',
  ANXIOUS: '焦虑',
  Anxious: '焦虑',
  REGRET: '后悔',
  Regret: '后悔',
  STRESSED: '压力',
  Stressed: '压力',
  NONE: '无明显情绪',
  None: '无明显情绪',
  PLEASED: '满意',
  Pleased: '满意',
  RELIEVED: '释然',
  Relieved: '释然',
  SURPRISED: '惊讶',
  Surprised: '惊讶',
  DISGUSTED: '厌恶',
  Disgusted: '厌恶',
  DEPRESSED: '沮丧',
  Depressed: '沮丧',
  GRIEVED: '悲伤',
  Grieved: '悲伤',
};

const localizeTag = (tag: TagOption, mapping: Record<string, string>) => ({
  ...tag,
  name: mapping[tag.code || ''] || mapping[tag.name] || tag.name,
});

export const getBehaviorTags = async () => {
  const response = await axios.get<TagOption[]>('api/tags/behavior');
  return response.data.map(tag => localizeTag(tag, behaviorNameMap));
};

export const getEmotionTags = async () => {
  const response = await axios.get<TagOption[]>('api/tags/emotion');
  return response.data.map(tag => localizeTag(tag, emotionNameMap));
};
