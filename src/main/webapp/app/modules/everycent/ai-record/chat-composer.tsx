import React, { useState } from 'react';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';

type ChatComposerProps = {
  disabled?: boolean;
  onSubmit: (text: string) => void;
  variant?: 'hero' | 'thread';
};

const ChatComposer = ({ disabled, onSubmit, variant = 'thread' }: ChatComposerProps) => {
  const [text, setText] = useState('');
  const canSubmit = text.trim().length > 0 && !disabled;

  const submit = () => {
    const value = text.trim();
    if (!value || disabled) return;

    onSubmit(value);
    setText('');
  };

  return (
    <div className={`everycent-chat-composer everycent-chat-composer--${variant}`}>
      <textarea
        value={text}
        rows={variant === 'hero' ? 2 : 1}
        disabled={disabled}
        placeholder="Ask anything"
        onChange={event => setText(event.target.value)}
        onKeyDown={event => {
          if (event.key === 'Enter' && !event.shiftKey) {
            event.preventDefault();
            submit();
          }
        }}
      />
      <div className="everycent-chat-composer__toolbar">
        <button type="button" className="everycent-chat-composer__ghost" aria-label="Add context">
          <FontAwesomeIcon icon="plus" />
        </button>
        <span className="everycent-chat-composer__tool">Tools</span>
        <button type="button" className="everycent-chat-composer__ghost" aria-label="Voice input">
          <FontAwesomeIcon icon="microphone" />
        </button>
        <button type="button" className="everycent-chat-composer__send" disabled={!canSubmit} onClick={submit} aria-label="Send message">
          <FontAwesomeIcon icon="arrow-left" rotation={180} />
        </button>
      </div>
    </div>
  );
};

export default ChatComposer;
