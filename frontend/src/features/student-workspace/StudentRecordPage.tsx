import { PageHeader } from '../../components/ui/PageHeader'
import { StudentIdentityNotice } from './StudentIdentityNotice'

export function StudentRecordPage({ section }: { section: 'profile' | 'gpa' }) {
  const title = section === 'profile' ? 'My profile' : 'GPA'
  const description = section === 'profile' ? 'Your student identity and profile details.' : 'GPA information returned by the academic grading service.'
  return <div className="role-workspace-page"><PageHeader eyebrow="Student portal" title={title} description={description} /><StudentIdentityNotice section={section} /></div>
}
