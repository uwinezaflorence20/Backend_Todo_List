const PRIORITY = {
  HIGH:   { cls: 'priority-high',   badge: 'badge-high',   label: 'High' },
  MEDIUM: { cls: 'priority-medium', badge: 'badge-medium', label: 'Medium' },
  LOW:    { cls: 'priority-low',    badge: 'badge-low',    label: 'Low' },
};

export default function TodoItem({ todo, onToggle, onEdit, onDelete }) {
  const p = PRIORITY[todo.priority] || PRIORITY.MEDIUM;
  const isOverdue = todo.dueDate && !todo.completed && new Date(todo.dueDate) < new Date();
  const dueLabel = todo.dueDate
    ? new Date(todo.dueDate).toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' })
    : null;

  return (
    <div className={`todo-card ${p.cls} ${todo.completed ? 'is-done' : ''}`}>
      <button
        className="todo-check-btn"
        onClick={() => onToggle(todo.id)}
        title={todo.completed ? 'Mark pending' : 'Mark complete'}
      >
        {todo.completed && '✓'}
      </button>

      <div className="todo-body">
        <div className="todo-title">{todo.title}</div>
        {todo.description && <div className="todo-desc">{todo.description}</div>}
        <div className="todo-meta">
          <span className={`badge ${p.badge}`}>{p.label}</span>
          {dueLabel && (
            <span className={`due-tag ${isOverdue ? 'overdue' : ''}`}>
              {isOverdue ? '⚠ Overdue · ' : '📅 '}{dueLabel}
            </span>
          )}
        </div>
      </div>

      <div className="todo-actions">
        <button className="icon-btn" onClick={() => onEdit(todo)} title="Edit">✏</button>
        <button className="icon-btn danger" onClick={() => onDelete(todo.id)} title="Delete">✕</button>
      </div>
    </div>
  );
}
