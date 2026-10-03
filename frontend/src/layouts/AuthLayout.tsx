import { Fragment, useEffect, useState } from 'react'
import { FieldCanvas } from '../components/charts/FieldCanvas'
import { Brand } from '../components/navigation/Brand'
import { Outlet } from 'react-router-dom'

const headline = 'Every class, mark and attendance record in one place.'
const rotatingPhrases = ['attendance', 'marks', 'enrolments', 'grading']
const prefersReducedMotion = () => typeof window !== 'undefined' && window.matchMedia('(prefers-reduced-motion: reduce)').matches

function AnimatedHeadline() {
  return (
    <h1>
      {headline.split(' ').map((word, index) => (
        <Fragment key={`${word}-${index}`}>
          <span className="login-word" style={{ '--i': index } as React.CSSProperties}>{word}</span>{' '}
        </Fragment>
      ))}
    </h1>
  )
}

function RotatingPhrase() {
  const [index, setIndex] = useState(0)

  useEffect(() => {
    if (prefersReducedMotion()) return
    const timer = window.setInterval(() => setIndex((current) => (current + 1) % rotatingPhrases.length), 2400)
    return () => window.clearInterval(timer)
  }, [])

  return (
    <p className="auth-rotline">
      Built for{' '}
      <span className="auth-rotline__slot">
        <span className="auth-rotline__phrase" key={index}>{rotatingPhrases[index]}</span>
      </span>
    </p>
  )
}

/** Weekly timetable artwork used on the sign-in panel. */
function TimetableArt() {
  const days = ['Mon', 'Tue', 'Wed', 'Thu', 'Fri']
  const blocks: [number, number, number][] = [
    [0, 0, 2], [0, 3, 1], [1, 1, 2], [1, 4, 1], [2, 0, 1],
    [2, 2, 2], [3, 1, 1], [3, 3, 2], [4, 0, 2], [4, 3, 1],
  ]

  return (
    <svg className="auth-timetable" viewBox="0 0 520 360" role="img" aria-label="A weekly class timetable filling in">
      {days.map((day, index) => (
        <text className="auth-timetable__label" key={day} x={64 + index * 98} y={24} textAnchor="middle">{day}</text>
      ))}
      <rect className="auth-timetable__grid" x={14} y={36} width={492} height={300} rx={14} />
      {blocks.map(([column, row, height], index) => (
        <rect
          className={`auth-timetable__block${index === 5 ? ' auth-timetable__block--highlight' : ''}`}
          key={`${column}-${row}-${height}`}
          style={{ '--i': index } as React.CSSProperties}
          x={24 + column * 98}
          y={40 + row * 62}
          width={80}
          height={height * 62 - 10}
          rx={8}
        />
      ))}
    </svg>
  )
}

export function AuthLayout() {
  return (
    <main className="auth-page">
      <section className="auth-form-panel" aria-label="Sign in to CampusCore">
        <FieldCanvas />
        <header className="auth-preview-brand"><Brand onCanvas /></header>
        <div className="auth-preview-intro">
          <span className="pill">Student academic management</span>
          <AnimatedHeadline />
          <RotatingPhrase />
          <p className="auth-preview-copy">Sign in to your university workspace to review classes, marks and attendance.</p>
        </div>
        <Outlet />
        <p className="auth-legal">CampusCore · Secure access for your campus community</p>
      </section>
      <aside className="auth-visual-panel">
        <div className="auth-visual-panel__caption">
          <span>Academic operations</span>
          <span>Connected by CampusCore</span>
        </div>
        <TimetableArt />
        <p className="auth-visual-panel__footer">Enrolment <i /> assessment <i /> attendance</p>
      </aside>
    </main>
  )
}