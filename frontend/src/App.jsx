import { useEffect, useState } from 'react'
import './App.css'

function lastSevenDays() {
  const days = []
  for (let i = 6; i >= 0; i--) {
    const d = new Date()
    d.setDate(d.getDate() - i)
    const iso = `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
    days.push({ iso, label: ['일', '월', '화', '수', '목', '금', '토'][d.getDay()] })
  }
  return days
}

export default function App() {
  const [habits, setHabits] = useState([])
  const [name, setName] = useState('')
  const [error, setError] = useState('')

  const [editingId, setEditingId] = useState(null)
  const [editName, setEditName] = useState('')

  const load = async () => {
    try {
      const res = await fetch('/api/habits')
      if (!res.ok) throw new Error()
      setHabits(await res.json())
      setError('')
    } catch {
      setError('서버에 연결할 수 없어요. 백엔드가 실행 중인지 확인하세요.')
    }
  }

  useEffect(() => {
    load()
  }, [])

  const addHabit = async (e) => {
    e.preventDefault()
    if (!name.trim()) return
    await fetch('/api/habits', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ name: name.trim() }),
    })
    setName('')
    load()
  }

  const toggle = async (habit) => {
    await fetch(`/api/habits/${habit.id}/check`, {
      method: habit.checkedToday ? 'DELETE' : 'POST',
    })
    load()
  }

  const toggleDate = async (habit, iso) => {
    const checked = habit.recentDates.includes(iso)
    await fetch(`/api/habits/${habit.id}/check?date=${iso}`, {
      method: checked ? 'DELETE' : 'POST',
    })
    load()
  }

  const remove = async (habit) => {

    const startEdit = (habit) => {
  setEditingId(habit.id)
  setEditName(habit.name)
}

const saveEdit = async (habit) => {
    const trimmed = editName.trim()
    setEditingId(null)
    if (!trimmed || trimmed === habit.name) return
    await fetch(`/api/habits/${habit.id}`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ name: trimmed }),
   })
   load()
  }

    if (!confirm(`'${habit.name}' 습관을 삭제할까요?`)) return
    await fetch(`/api/habits/${habit.id}`, { method: 'DELETE' })
    load()
  }

  return (
    <div className="app">
      <h1>습관 트래커</h1>

      <form onSubmit={addHabit} className="add-form">
        <input
          value={name}
          onChange={(e) => setName(e.target.value)}
          placeholder="새 습관 (예: 운동, 독서)"
        />
        <button type="submit">추가</button>
      </form>

      {error && <p className="error">{error}</p>}

      <ul className="habit-list">
        {habits.map((h) => (
          <li key={h.id} className={h.checkedToday ? 'done' : ''}>
            <button className="check" onClick={() => toggle(h)}>
              {h.checkedToday ? '✓' : ''}
            </button>
            <div className="info">
              {editingId === h.id ? (
        <input
        className="edit-input"
        value={editName}
        autoFocus
        onChange={(e) => setEditName(e.target.value)}
       onBlur={() => saveEdit(h)}
        onKeyDown={(e) => {
        if (e.key === 'Enter') e.target.blur()
         if (e.key === 'Escape') setEditingId(null)
       }}
      />
    ) : (
  <span className="name" title="클릭해서 이름 수정" onClick={() => startEdit(h)}>
    {h.name}
  </span>
)} 
              <div className="week">
                {lastSevenDays().map((d) => (
                  <button
                    key={d.iso}
                    className={h.recentDates.includes(d.iso) ? 'dot on' : 'dot'}
                    title={d.iso}
                    onClick={() => toggleDate(h, d.iso)}
                  >
                    {d.label}
                  </button>
                ))}
              </div>
            </div>
            <span className="streak">🔥 {h.streak}일</span>
            <button className="delete" onClick={() => remove(h)}>삭제</button>
          </li>
        ))}
      </ul>

      {habits.length === 0 && !error && (
        <p className="empty">아직 습관이 없어요. 위에서 추가해 보세요!</p>
      )}
    </div>
  )
}