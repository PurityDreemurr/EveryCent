import './tags.scss';

import React, { useEffect, useState } from 'react';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';

import EmotionTagBadge from '../shared/emotion-tag-badge';
import { getBehaviorTags, getEmotionTags, TagOption } from '../transaction/tag-api';

const TagList = ({
  icon,
  title,
  description,
  loading,
  variant,
  tags,
}: {
  icon: 'flag' | 'heart';
  title: string;
  description: string;
  loading: boolean;
  variant?: 'default' | 'emotion';
  tags: TagOption[];
}) => (
  <article className="everycent-panel everycent-tags-page__panel">
    <header className="everycent-tags-page__section-header">
      <span className="everycent-tags-page__section-icon">
        <FontAwesomeIcon icon={icon} />
      </span>
      <div>
        <h2>{title}</h2>
        <p>{description}</p>
      </div>
    </header>

    {loading ? (
      <div className="everycent-tags-page__empty">标签加载中...</div>
    ) : tags.length === 0 ? (
      <div className="everycent-tags-page__empty">暂无标签</div>
    ) : (
      <div className="everycent-tags-page__items">
        {tags.map(tag => (
          <span key={tag.id} className={`everycent-tags-page__tag${variant === 'emotion' ? ' everycent-tags-page__tag--emotion' : ''}`}>
            {variant === 'emotion' ? <EmotionTagBadge name={tag.name} size="lg" /> : <strong>{tag.name}</strong>}
          </span>
        ))}
      </div>
    )}
  </article>
);

const TagsPage = () => {
  const [behaviorTags, setBehaviorTags] = useState<TagOption[]>([]);
  const [emotionTags, setEmotionTags] = useState<TagOption[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let mounted = true;

    const loadTags = async () => {
      setLoading(true);
      setError(null);
      try {
        const [behavior, emotion] = await Promise.all([getBehaviorTags(), getEmotionTags()]);
        if (mounted) {
          setBehaviorTags(behavior);
          setEmotionTags(emotion);
        }
      } catch (err) {
        if (mounted) {
          setError('标签加载失败，请检查后端标签接口和登录状态。');
        }
      } finally {
        if (mounted) {
          setLoading(false);
        }
      }
    };

    loadTags();

    return () => {
      mounted = false;
    };
  }, []);

  return (
    <div className="everycent-page everycent-tags-page">
      <div className="everycent-page__header">
        <div>
          <h1 className="everycent-page__title">标签</h1>
          <p className="everycent-page__subtitle">查看系统当前可用于收支记录和 AI 记账解析的行为标签、情绪标签。</p>
        </div>
      </div>

      {error && <div className="everycent-tags-page__alert">{error}</div>}

      <section className="everycent-tags-page__grid">
        <TagList icon="flag" title="行为标签" description="用于描述消费或收入发生的场景。" loading={loading} tags={behaviorTags} />
        <TagList
          icon="heart"
          title="情绪标签"
          description="用于记录一笔收支背后的情绪状态。"
          loading={loading}
          tags={emotionTags}
          variant="emotion"
        />
      </section>
    </div>
  );
};

export default TagsPage;
