import './emotion-tag-badge.scss';

import React from 'react';

import { getEmotionImage } from './emotion-assets';

type EmotionTagBadgeProps = {
  className?: string;
  name?: string | number;
  showText?: boolean;
  size?: 'sm' | 'md' | 'lg';
};

const EmotionTagBadge = ({ className = '', name, showText = true, size = 'md' }: EmotionTagBadgeProps) => {
  const label = name === undefined || name === null || name === '' ? '未知情绪' : String(name);
  const image = getEmotionImage(label);

  return (
    <span className={`everycent-emotion-badge everycent-emotion-badge--${size} ${className}`.trim()}>
      {image && <img src={image} alt={label} loading="lazy" />}
      {showText && <strong>{label}</strong>}
    </span>
  );
};

export default EmotionTagBadge;
