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

  const handleSubmit = (e) => {
    e.preventDefault();
    onSave({ ...form, dueDate: form.dueDate || null });
  };

  return (
    <form className="todo-form" onSubmit={handleSubmit}>
      <div className="form-group">
        <label>Title *</label>
        <input
          type="text"
          value={form.title}
          onChange={(e) => setForm({ ...form, title: e.target.value })}
          placeholder="What needs to be done?"
          required
        />
      </div>
      <div className="form-group">
        <label>Description</label>
        <textarea
          value={form.description}
          onChange={(e) => setForm({ ...form, description: e.target.value })}
          placeholder="Add details (optional)"
          rows={3}
        />
      </div>
      <div className="form-row">
        <div className="form-group">
          <label>Priority</label>
          <select value={form.priority} onChange={(e) => setForm({ ...form, priority: e.target.value })}>
            <option value="LOW">Low</option>
            <option value="MEDIUM">Medium</option>
            <option value="HIGH">High</option>
          </select>
        </div>
        <div className="form-group">
          <label>Due Date</label>
          <input
            type="date"
            value={form.dueDate}
            onChange={(e) => setForm({ ...form, dueDate: e.target.value })}
          />
        </div>
      </div>
      <div className="form-actions">
        <button type="submit" className="btn btn-primary">{initial ? 'Update' : 'Add Todo'}</button>
        <button type="button" className="btn btn-ghost" onClick={onCancel}>Cancel</button>
      </div>
    </form>
  );
}
