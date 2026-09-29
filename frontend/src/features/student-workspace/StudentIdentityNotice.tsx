import { ArrowRight, BookOpen, CircleHelp } from 'lucide-react'
import { Link } from 'react-router-dom'
import { Card } from '../../components/ui/Card'

const copy = {
  dashboard: {
    title: 'Link your student record to get started',
    body: 'You are signed in, but CampusCore cannot yet match this account to a student profile. It will not assume your account ID is the same as your student ID.',
    next: 'Ask your campus administrator to verify that this account is linked to your student profile.',
  },
  profile: {
    title: 'Your student profile is not linked yet',
    body: 'CampusCore cannot safely match this sign-in to a student profile, so personal profile information is not available here.',
    next: 'Ask your campus administrator to verify the account-to-profile link.',
  },
  gpa: {
    title: 'An enrollment link is required for GPA',
    body: 'GPA information is returned for an enrollment. This account session does not identify a student enrollment, so CampusCore cannot request or display a GPA yet.',
    next: 'Ask your campus administrator to verify your student profile and enrollment are linked to this account.',
  },
} satisfies Record<'dashboard' | 'profile' | 'gpa', { title: string; body: string; next: string }>

export function StudentIdentityNotice({ section }: { section: 'dashboard' | 'profile' | 'gpa' }) {
  const notice = copy[section]
  return (
    <Card as="section" className="identity-availability" role="status" aria-labelledby="identity-availability-title">
      <span className="identity-availability__icon"><CircleHelp size={21} aria-hidden="true" /></span>
      <div className="identity-availability__copy">
        <span className="identity-availability__badge">Setup required</span>
        <p className="eyebrow">Account setup</p>
        <h2 id="identity-availability-title">{notice.title}</h2>
        <p>{notice.body}</p>
        <div className="identity-availability__next-step">
          <strong>Next step</strong>
          <p>{notice.next}</p>
        </div>
        <Link to="/student/courses"><BookOpen size={16} aria-hidden="true" /> Browse the available course catalog <ArrowRight size={15} aria-hidden="true" /></Link>
      </div>
    </Card>
  )
}
