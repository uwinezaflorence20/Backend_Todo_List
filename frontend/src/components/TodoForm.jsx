import { useState, useEffect } from 'react';

const EMPTY = { title: '', description: '', priority: 'MEDIUM', dueDate: '' };

export default function TodoForm({ initial, onSave, onCancel }) {
  const [form, setForm] = useState(EMPTY);

  useEffect(() => {
    if (initial) {
      setForm({
        title: initial.title || '',
        description: initial.description || '',
        priority: initial.priority || 'MEDIUM',
        dueDate: initial.dueDate ? initial.dueDate.slice(0, 10) : '',
      });
    } else {
      setForm(EMPTY);
    }
  }, [initial]);

  const set = (key) => (e) => setForm((f) => ({ ...f, [key]: e.target.value }));

  const handleSubmit = (e) => {
    e.preventDefault();
    onSave({ ...form, dueDate: form.dueDate || null });
  };

  return (
    <div className="todo-form-card">
      <div className="todo-form-title">{initial ? 'Edit todo' : 'New todo'}</div>
      <form onSubmit={handleSubmit}>
        <div className="form-group">
          <label className="form-label">Title *</label>
          <input
            className="form-input"
            type="text"
            value={form.title}
            onChange={set('title')}
            placeholder="What needs to be done?"
            required
            autoFocus
          />
        </div>
        <div className="form-group">
          <label className="form-label">Description</label>
          <textarea
            className="form-textarea"
            value={form.description}
            onChange={set('description')}
            placeholder="Add more details (optional)"
            rows={2}
          />
        </div>
        <div className="form-row">
          <div className="form-group" style={{ marginBottom: 0 }}>
            <label className="form-label">Priority</label>
            <select className="form-select" value={form.priority} onChange={set('priority')}>
              <option value="LOW">🟢 Low</option>
              <option value="MEDIUM">🟡 Medium</option>
              <option value="HIGH">🔴 High</option>
            </select>
          </div>
          <div className="form-group" style={{ marginBottom: 0 }}>
            <label className="form-label">Due date</label>
            <input
              className="form-input"
              type="date"
              value={form.dueDate}
              onChange={set('dueDate')}
            />
          </div>
        </div>
        <div className="form-actions">
          <button type="submit" className="btn btn-primary">
            {initial ? 'Save changes' : 'Add todo'}
          </button>
          <button type="button" className="btn btn-ghost" onClick={onCancel}>
            Cancel
          </button>
        </div>
      </form>
    </div>
  );
}
