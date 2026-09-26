import { useEffect, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { MessageCircle, Send, X, Sparkles } from 'lucide-react';
import api, { errMsg } from '../api';
import { addDays } from '../utils';

const QUICK = [
  'Cheapest way from Hyderabad to Goa this weekend',
  'Resorts in Goa under ₹9000',
  'Train from Chennai to Bengaluru tomorrow',
  'Where can I go from Hyderabad under ₹2500?',
];

const GREETING = {
  role: 'assistant',
  content:
    "Hi, I'm the TripEase assistant. Tell me where you want to go, when, and your budget. I'll compare flights, trains, buses and stays for you.",
  suggestions: [],
};

export default function ChatWidget() {
  const nav = useNavigate();
  const [open, setOpen] = useState(false);
  const [messages, setMessages] = useState([GREETING]);
  const [input, setInput] = useState('');
  const [busy, setBusy] = useState(false);
  const [engine, setEngine] = useState(null);
  const bottom = useRef(null);

  useEffect(() => {
    bottom.current?.scrollIntoView({ behavior: 'smooth', block: 'end' });
  }, [messages, busy, open]);

  const send = async (text) => {
    const message = text.trim();
    if (!message || busy) return;
    const history = messages.slice(1).slice(-8).map(({ role, content }) => ({ role, content }));
    setMessages((m) => [...m, { role: 'user', content: message }]);
    setInput('');
    setBusy(true);
    try {
      const res = await api.post('/api/ai/chat', { message, history });
      setEngine(res.data.source);
      setMessages((m) => [...m, { role: 'assistant', content: res.data.reply, suggestions: res.data.suggestions || [] }]);
    } catch (e) {
      setMessages((m) => [...m, { role: 'assistant', content: errMsg(e), suggestions: [], failed: true }]);
    } finally {
      setBusy(false);
    }
  };

  const go = (s) => {
    const enc = encodeURIComponent;
    if (s.mode === 'HOTEL') {
      nav(`/hotels?city=${enc(s.to)}&checkIn=${s.date}&checkOut=${addDays(s.date, 2)}&rooms=1`);
    } else {
      nav(`/search/${s.mode.toLowerCase()}?from=${enc(s.from)}&to=${enc(s.to)}&date=${s.date}&pax=1`);
    }
    setOpen(false);
  };

  return (
    <>
      {!open && (
        <button className="chat-fab" onClick={() => setOpen(true)} aria-label="Open travel assistant">
          <MessageCircle size={22} aria-hidden="true" />
          <span>Ask TripEase</span>
        </button>
      )}

      {open && (
        <section className="chat-panel" aria-label="Travel assistant">
          <header className="chat-head">
            <div>
              <strong>Travel assistant</strong>
              <small>
                <Sparkles size={12} aria-hidden="true" />{' '}
                {engine && engine !== 'rule-based' ? `Powered by ${engine} and live fares` : 'Plans trips using live fares'}
              </small>
            </div>
            <button onClick={() => setOpen(false)} aria-label="Close assistant">
              <X size={20} />
            </button>
          </header>

          <div className="chat-body" aria-live="polite">
            {messages.map((m, i) => (
              <div key={i} className={`msg msg-${m.role} ${m.failed ? 'msg-failed' : ''}`}>
                <p>{m.content}</p>
                {m.suggestions?.length > 0 && (
                  <div className="msg-chips">
                    {m.suggestions.map((s, j) => (
                      <button key={j} className="chip" onClick={() => go(s)}>{s.label}</button>
                    ))}
                  </div>
                )}
              </div>
            ))}
            {messages.length === 1 && (
              <div className="msg-chips quick">
                {QUICK.map((q) => (
                  <button key={q} className="chip" onClick={() => send(q)}>{q}</button>
                ))}
              </div>
            )}
            {busy && (
              <div className="msg msg-assistant typing" aria-label="Assistant is typing">
                <i /><i /><i />
              </div>
            )}
            <div ref={bottom} />
          </div>

          <form className="chat-input" onSubmit={(e) => { e.preventDefault(); send(input); }}>
            <input
              value={input}
              onChange={(e) => setInput(e.target.value)}
              placeholder="Where do you want to go?"
              maxLength={500}
              aria-label="Message"
            />
            <button type="submit" disabled={busy || !input.trim()} aria-label="Send message">
              <Send size={18} />
            </button>
          </form>
        </section>
      )}
    </>
  );
}
