import { Link } from 'react-router-dom'
import { ArrowRight, CircleHelp } from 'lucide-react'
import { Card } from '../../components/ui/Card'

export function StudentIdentityNotice({ section }: { section: 'dashboard' | 'profile' | 'gpa' }) {
  const copy = section === 'dashboard'
    ? { title: 'Your student record could not be identified', body: 'The student dashboard requires a student ID. Your signed-in account provides a user ID, and the API does not provide a safe way to map it to your student record in this session.' }
    : section === 'profile'
      ? { title: 'Student profile is unavailable in this session', body: 'The API can return a student profile by student ID, but your account response does not include that ID. CampusCore will not assume the user ID and student ID are the same.' }
      : { title: 'GPA details are unavailable in this session', body: 'GPA lookup requires an enrollment ID. The current student APIs do not provide your enrollment IDs without first resolving your student record.' }
  return (
    <Card className="identity-availability" role="status">
      <span className="identity-availability__icon"><CircleHelp size={21} aria-hidden="true" /></span>
      <div className="identity-availability__copy">
        <p className="eyebrow">Account identity</p>
        <h2>{copy.title}</h2>
        <p>{copy.body}</p>
        <p>If you believe your account should be linked, contact your campus administrator to verify the student profile association.</p>
        {section !== 'dashboard' && <Link to="/student/dashboard">Return to overview <ArrowRight size={15} aria-hidden="true" /></Link>}
      </div>
    </Card>
  )
}
