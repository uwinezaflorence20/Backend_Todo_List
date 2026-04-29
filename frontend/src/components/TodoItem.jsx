const PRIORITY_CLASS = { LOW: 'priority-low', MEDIUM: 'priority-medium', HIGH: 'priority-high' };

export default function TodoItem({ todo, onToggle, onEdit, onDelete }) {
  const overdue = todo.dueDate && !todo.completed && new Date(todo.dueDate) < new Date();

  return (
    <div className={`todo-item ${todo.completed ? 'completed' : ''}`}>
      <button className="todo-check" onClick={() => onToggle(todo.id)} title="Toggle complete">
        {todo.completed ? '✓' : ''}
      </button>
      <div className="todo-body">
        <span className="todo-title">{todo.title}</span>
        {todo.description && <span className="todo-desc">{todo.description}</span>}
        <div className="todo-meta">
          <span className={`badge ${PRIORITY_CLASS[todo.priority]}`}>{todo.priority}</span>
          {todo.dueDate && (
            <span className={`due-date ${overdue ? 'overdue' : ''}`}>
              {overdue ? '⚠ ' : ''}Due {new Date(todo.dueDate).toLocaleDateString()}
            </span>
          )}
        </div>
      </div>
      <div className="todo-actions">
        <button className="btn-icon" onClick={() => onEdit(todo)} title="Edit">✏</button>
        <button className="btn-icon btn-icon-danger" onClick={() => onDelete(todo.id)} title="Delete">✕</button>
      </div>
    </div>
  );
}
