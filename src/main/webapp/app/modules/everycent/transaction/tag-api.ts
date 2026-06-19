import axios from 'axios';

export interface TagOption {
  id: number;
  name: string;
}

const behaviorNameMap: Record<string, string> = {
  Food: '餐饮',
  Transport: '交通',
  Shopping: '购物',
  Entertainment: '娱乐',
  Study: '学习',
  Medical: '医疗',
  Salary: '工资',
  'Part time': '兼职',
  Other: '其他',
};

const emotionNameMap: Record<string, string> = {
  Happy: '开心',
  Calm: '平静',
  Impulsive: '冲动',
  Anxious: '焦虑',
  Regret: '后悔',
  Stressed: '压力',
  None: '无明显情绪',
};

const localizeTag = (tag: TagOption, mapping: Record<string, string>) => ({
  ...tag,
  name: mapping[tag.name] || tag.name,
});

export const getBehaviorTags = async () => {
  const response = await axios.get<TagOption[]>('api/tags/behavior');
  return response.data.map(tag => localizeTag(tag, behaviorNameMap));
};

export const getEmotionTags = async () => {
  const response = await axios.get<TagOption[]>('api/tags/emotion');
  return response.data.map(tag => localizeTag(tag, emotionNameMap));
};
