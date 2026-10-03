import { LegalDocument, LegalList, LegalListItem, LegalParagraph } from './LegalDocument'

export const TERMS_CONTACT_EMAIL = 'adarsha2346s@gmail.com'

const sections = [
  {
    id: 'acceptance',
    heading: 'Acceptance of terms',
    body: (
      <>
        <LegalParagraph>
          These Terms and Conditions govern access to CampusCore. By signing in or otherwise using CampusCore you
          accept these terms. If you do not accept them, do not use the service, and contact your campus
          administrator or the operator if you believe access was issued in error.
        </LegalParagraph>
        <LegalParagraph>
          These terms apply to the individual using an account. A campus or institution that operates a deployment is
          responsible for the instructions it gives to its own users.
        </LegalParagraph>
      </>
    ),
  },
  {
    id: 'purpose',
    heading: 'Purpose of CampusCore',
    body: (
      <>
        <LegalParagraph>
          CampusCore is a Student Academic Management System. It is an academic and project platform used to manage
          account and profile information together with academic information such as courses, enrollment,
          assessments, marks, attendance, grading and related records.
        </LegalParagraph>
        <LegalParagraph>
          CampusCore is not an official system of record of any university, and information entered in it should not be
          treated as an official transcript or an official certification.
        </LegalParagraph>
      </>
    ),
  },
  {
    id: 'accounts-and-credentials',
    heading: 'Accounts and credentials',
    body: (
      <>
        <LegalParagraph>
          Access is granted per account. You are responsible for the activity that happens under your account, for
          keeping your password confidential, and for signing out on shared or public devices.
        </LegalParagraph>
        <LegalList>
          <LegalListItem>Credentials are personal. Do not share your username or password, in any form, with anyone.</LegalListItem>
          <LegalListItem>Use only the credentials issued to you; do not attempt to use, guess or take over another account.</LegalListItem>
          <LegalListItem>Tell the operator promptly if you believe your credentials have been disclosed or used without your permission.</LegalListItem>
          <LegalListItem>An account that is inactive cannot sign in. Restoration is handled by an administrator.</LegalListItem>
        </LegalList>
      </>
    ),
  },
  {
    id: 'authorized-use',
    heading: 'Authorized use',
    body: (
      <>
        <LegalParagraph>
          Use CampusCore only for legitimate academic and administrative purposes within your campus, and only for
          records you are authorized to create, view or change. Where you are unsure whether you are authorized to
          record something, ask before you record it.
        </LegalParagraph>
        <LegalParagraph>
          Do not enter information you are not authorized to provide, and do not use CampusCore to collect or process
          information about people beyond what your role requires. Sensitive personal information, such as health
          records, government identifiers or payment card details, does not belong in CampusCore fields.
        </LegalParagraph>
      </>
    ),
  },
  {
    id: 'academic-records',
    heading: 'Academic records and user responsibilities',
    body: (
      <>
        <LegalParagraph>
          Academic information is meaningful only when it is accurate. When you create or change an academic record,
          you are responsible for its correctness, including the relationship between a person, a course, an
          enrollment, an assessment, a mark, an attendance record or a grading policy.
        </LegalParagraph>
        <LegalList>
          <LegalListItem>Record marks, attendance and assessment information only against the correct enrollment or session.</LegalListItem>
          <LegalListItem>Do not backdate, duplicate or overwrite a record to influence a calculated grade, result or attendance rate.</LegalListItem>
          <LegalListItem>Correct an error through the normal application action where one exists, and tell your administrator when one does not.</LegalListItem>
          <LegalListItem>Treat academic records you can see as confidential campus data, not as information for public circulation.</LegalListItem>
        </LegalList>
      </>
    ),
  },
  {
    id: 'role-based-access',
    heading: 'Role-based access',
    body: (
      <>
        <LegalParagraph>
          CampusCore has three roles, and each role exposes a different set of records:
        </LegalParagraph>
        <LegalList>
          <LegalListItem>Admin accounts maintain directory, academic and operational records across the deployment.</LegalListItem>
          <LegalListItem>Faculty accounts read the academic directories exposed to them.</LegalListItem>
          <LegalListItem>Student accounts see their own profile, courses, attendance and grade information.</LegalListItem>
        </LegalList>
        <LegalParagraph>
          These limits are enforced on the server. Attempting to reach a record that your role does not cover is a
          breach of these terms, whether or not the attempt succeeds. If your role is wrong, request a correction
          instead of working around it.
        </LegalParagraph>
      </>
    ),
  },
  {
    id: 'prohibited-misuse',
    heading: 'Prohibited misuse',
    body: (
      <>
        <LegalParagraph>The following are not permitted:</LegalParagraph>
        <LegalList>
          <LegalListItem>accessing, altering or deleting an account, record or report without authorization;</LegalListItem>
          <LegalListItem>bypassing, disabling or working around role checks, or attempting to obtain a higher role;</LegalListItem>
          <LegalListItem>probing, scanning, overloading or otherwise interfering with the service or its infrastructure;</LegalListItem>
          <LegalListItem>uploading malicious code, or introducing content intended to damage or disrupt the platform;</LegalListItem>
          <LegalListItem>reproducing, redistributing or reselling access to the service, or copying academic records for purposes outside your authorization;</LegalListItem>
          <LegalListItem>harassing other users, or using CampusCore to store or distribute unlawful or abusive material.</LegalListItem>
        </LegalList>
      </>
    ),
  },
  {
    id: 'intellectual-property',
    heading: 'Intellectual property',
    body: (
      <>
        <LegalParagraph>
          The CampusCore software, its design and its written materials remain the property of their respective
          authors. These terms grant you a limited, revocable, non-exclusive right to use the service for its intended
          academic purpose. Nothing here grants any right to the underlying software or to copy it, rebrand it, or
          offer it as your own service without permission.
        </LegalParagraph>
        <LegalParagraph>
          Academic information belongs to the people and institutions it describes. Nothing in these terms transfers
          ownership of that information.
        </LegalParagraph>
      </>
    ),
  },
  {
    id: 'availability',
    heading: 'Availability and limitations',
    body: (
      <>
        <LegalParagraph>
          CampusCore is provided on hosted infrastructure on an as-available basis, without a guarantee of
          uninterrupted operation. Maintenance, infrastructure changes and third-party provider limitations can
          interrupt the service, and a free hosting tier may be suspended after a period of inactivity.
        </LegalParagraph>
        <LegalParagraph>
          Grade calculations, attendance percentages and results depend on the records entered and on the grading
          policies configured for the deployment. Verify important results against your institution's official
          records before acting on them.
        </LegalParagraph>
      </>
    ),
  },
  {
    id: 'changes-to-the-service',
    heading: 'Changes to the service',
    body: (
      <LegalParagraph>
        Features, routes, record types and hosting arrangements may change. Where a change affects functionality you
        rely on, the deployment may be updated to describe it. Continued use after such a change means the updated
        terms apply from that point.
      </LegalParagraph>
    ),
  },
  {
    id: 'termination',
    heading: 'Account and access termination',
    body: (
      <>
        <LegalParagraph>
          An administrator may deactivate an account, and the operator may suspend or withdraw access where these
          terms are not followed, where information has been recorded without authorization, or where the service is
          no longer operated. Deactivation prevents sign-in but does not remove records already recorded.
        </LegalParagraph>
        <LegalParagraph>
          To request that an account or a record be corrected or removed, contact the operator at the address below.
          Where a record cannot be removed through the application, handling may require action by an administrator.
        </LegalParagraph>
      </>
    ),
  },
  {
    id: 'contact',
    heading: 'Contact',
    body: (
      <>
        <LegalParagraph>
          Questions about these terms can be sent to{' '}
          <a href={`mailto:${TERMS_CONTACT_EMAIL}`}>{TERMS_CONTACT_EMAIL}</a>. Please include the username concerned,
          the role you sign in with, and a short description of the request. Do not send passwords or tokens by
          email.
        </LegalParagraph>
      </>
    ),
  },
]

export function TermsConditionsPage() {
  return (
    <LegalDocument
      title="Terms & Conditions"
      summary="The rules for using CampusCore, including accounts, authorized use of academic records, role limits and termination."
      sections={sections}
    />
  )
}