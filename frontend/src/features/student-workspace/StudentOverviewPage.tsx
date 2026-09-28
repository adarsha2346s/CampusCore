import { PageHeader } from '../../components/ui/PageHeader'
import { StudentIdentityNotice } from './StudentIdentityNotice'

export function StudentOverviewPage() {
  return <div className="role-workspace-page"><PageHeader eyebrow="Student portal" title="Overview" description="Your academic dashboard is available after your account is linked to a student record." /><StudentIdentityNotice section="dashboard" /></div>
}
