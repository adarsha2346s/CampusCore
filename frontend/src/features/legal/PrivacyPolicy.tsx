import { LegalDocument, LegalList, LegalListItem, LegalParagraph } from './LegalDocument'

export const PRIVACY_CONTACT_EMAIL = 'adarsha2346s@gmail.com'

const sections = [
  {
    id: 'introduction',
    heading: 'Introduction',
    body: (
      <>
        <LegalParagraph>
          CampusCore is a Student Academic Management System. It is an academic and project platform that helps a
          campus keep track of people, courses and academic records. This Privacy Policy explains what information
          CampusCore handles, why it is handled, and who can see it.
        </LegalParagraph>
        <LegalParagraph>
          The operator of this deployment can be contacted at{' '}
          <a href={`mailto:${PRIVACY_CONTACT_EMAIL}`}>{PRIVACY_CONTACT_EMAIL}</a>. If you have a privacy question,
          use that address and describe the account or record involved.
        </LegalParagraph>
      </>
    ),
  },
  {
    id: 'information-collected',
    heading: 'Information collected',
    body: (
      <>
        <LegalParagraph>
          CampusCore does not ask for information that a role does not need. Depending on the action you take, the
          information you provide may include the following.
        </LegalParagraph>
        <LegalList>
          <LegalListItem>Account information: a username, an email address, a password, an account role and an active or inactive status.</LegalListItem>
          <LegalListItem>Profile information: for a student, a first name, last name, enrollment number, date of birth, phone number, admission year and status; for faculty, a first name, last name, employee number and phone number.</LegalListItem>
          <LegalListItem>Academic information: departments, courses, credit values and capacity, enrollments with a semester and academic year, assessments with marks and weights, marks recorded against assessments, attendance sessions and individual attendance records, grading policies and calculated grade results.</LegalListItem>
          <LegalListItem>Sign-in activity: a record of certain administrative operations, such as which entity was created or changed, is written to an audit log.</LegalListItem>
          <LegalListItem>Technical information: requests handled by the server, including IP address and user agent as they appear in ordinary server logs.</LegalListItem>
        </LegalList>
        <LegalParagraph>
          CampusCore is not designed to receive sensitive personal information such as health records, government
          identifiers, payment card details or special-category data. Please do not submit such information through
          account, profile or academic fields.
        </LegalParagraph>
      </>
    ),
  },
  {
    id: 'academic-and-account-information',
    heading: 'Academic and account information',
    body: (
      <>
        <LegalParagraph>
          Account and academic information is entered by administrators, faculty and students through the application
          itself; CampusCore does not import it from an external directory. Academic records are linked to a person
          through the student or faculty profile, and marks, attendance and grading results are linked to an
          enrollment.
        </LegalParagraph>
        <LegalParagraph>
          Please enter only information you are authorized to record. Do not enter data about another person unless
          your role and your campus administrator allow it. Inaccurate entries can affect grades, attendance
          calculations and access decisions.
        </LegalParagraph>
      </>
    ),
  },
  {
    id: 'use-of-information',
    heading: 'How information is used',
    body: (
      <>
        <LegalParagraph>CampusCore uses the information it holds to:</LegalParagraph>
        <LegalList>
          <LegalListItem>authenticate users and maintain a signed-in session;</LegalListItem>
          <LegalListItem>apply role-based access to directories, academic records and administrative operations;</LegalListItem>
          <LegalListItem>calculate and display academic results such as grade points, percentages and attendance rates;</LegalListItem>
          <LegalListItem>maintain an audit trail of recorded administrative changes; and</LegalListItem>
          <LegalListItem>diagnose faults and keep the service running.</LegalListItem>
        </LegalList>
        <LegalParagraph>
          CampusCore does not sell information, does not use academic records for advertising, and does not build
          behavioural profiles.
        </LegalParagraph>
      </>
    ),
  },
  {
    id: 'authentication-and-security',
    heading: 'Authentication and security',
    body: (
      <>
        <LegalParagraph>
          CampusCore authenticates with a username and a password. Passwords are stored as BCrypt hashes rather than
          as readable text, so a password cannot be recovered from the database; it can only be replaced. After a
          successful sign-in the server issues a signed JSON Web Token (JWT) that carries the username and role, and
          the browser keeps that token in memory only for the current tab. It is not written to browser storage, so
          closing or reloading the page ends the session and requires a new sign-in. Tokens expire after a limited
          lifetime, which by default is around 24 hours and is set by the deployment.
        </LegalParagraph>
        <LegalParagraph>
          Access to records is enforced on the server by role, not only by the interface. The measures described here
          reduce risk but do not guarantee that no unauthorized access can ever occur. Users must keep credentials
          confidential and report suspected compromise as soon as possible.
        </LegalParagraph>
      </>
    ),
  },
  {
    id: 'storage-and-infrastructure',
    heading: 'Data storage and infrastructure',
    body: (
      <>
        <LegalParagraph>
          This deployment runs on hosted infrastructure. The web interface is served by Vercel, the application
          server is hosted on Render, and academic data is stored in a managed MariaDB-compatible database reached by
          the application server. Data is therefore held on infrastructure operated by third parties and is
          transmitted over the network using the transport security those platforms provide.
        </LegalParagraph>
        <LegalParagraph>
          Access to that infrastructure is limited to the deployment operator and the platform providers, and follows
          the access controls those platforms provide. Information should be treated as confidential campus data
          rather than as public information.
        </LegalParagraph>
      </>
    ),
  },
  {
    id: 'access-and-permissions',
    heading: 'Access and role-based permissions',
    body: (
      <>
        <LegalParagraph>
          CampusCore has three roles, and the role attached to an account decides which records are visible:
        </LegalParagraph>
        <LegalList>
          <LegalListItem>Admin accounts can view and maintain directory, academic and operational records across the deployment.</LegalListItem>
          <LegalListItem>Faculty accounts can read the academic directories that are exposed to them.</LegalListItem>
          <LegalListItem>Student accounts can see their own profile, courses, attendance and grade information.</LegalListItem>
        </LegalList>
        <LegalParagraph>
          Every request is checked against the role on the server. A user who believes their role or the records they
          can see are incorrect should contact the operator at the address below rather than attempting to work around
          the restriction.
        </LegalParagraph>
      </>
    ),
  },
  {
    id: 'data-retention',
    heading: 'Data retention',
    body: (
      <>
        <LegalParagraph>
          CampusCore does not apply a fixed, automated retention or deletion schedule. In the current implementation,
          account and academic records are retained in the database for as long as the deployment is operated, and are
          removed only through the operations a role provides, such as deactivating an account or deleting a record
          that has no dependent records. Some records cannot be deleted while related records exist.
        </LegalParagraph>
        <LegalParagraph>
          Deactivating an account prevents sign-in but does not erase the account row or the academic records already
          linked to it. If information must be corrected or removed, contact the operator; removal may require manual
          action in the database or a change to the deployment.
        </LegalParagraph>
      </>
    ),
  },
  {
    id: 'third-party-infrastructure',
    heading: 'Third-party infrastructure',
    body: (
      <>
        <LegalParagraph>
          CampusCore relies on the hosting and database providers listed above to deliver the service. Those providers
          process information strictly in order to run the platform, under their own published terms and privacy
          policies, which also govern the handling of this information on their side.
        </LegalParagraph>
        <LegalParagraph>
          CampusCore does not embed third-party advertising, analytics or social widgets. It does not use tracking
          cookies, and it does not set its own analytics or marketing cookies in the browser.
        </LegalParagraph>
      </>
    ),
  },
  {
    id: 'user-responsibilities',
    heading: 'User responsibilities',
    body: (
      <>
        <LegalParagraph>By using CampusCore you agree to the following:</LegalParagraph>
        <LegalList>
          <LegalListItem>use only the credentials issued to you, and keep your password confidential;</LegalListItem>
          <LegalListItem>do not share your credentials or attempt to use an account that is not yours;</LegalListItem>
          <LegalListItem>enter only information you are authorized to record, and record it accurately;</LegalListItem>
          <LegalListItem>report a lost password, suspected unauthorized access or incorrect information as soon as you can; and</LegalListItem>
          <LegalListItem>make sure that shared devices and browsers are not left signed in.</LegalListItem>
        </LegalList>
        <LegalParagraph>
          CampusCore is an academic and project platform. It is not an official system of record of any university,
          and entries made in it should be treated as academic platform data rather than as an official transcript.
        </LegalParagraph>
      </>
    ),
  },
  {
    id: 'changes-to-this-policy',
    heading: 'Changes to this policy',
    body: (
      <LegalParagraph>
        This policy may be updated when the application, the hosting arrangements or the applicable requirements
        change. The revision date at the top of this page shows when it was last updated. Continued use of CampusCore
        after an update means the updated policy applies from that point. Material questions can be raised at the
        contact address below.
      </LegalParagraph>
    ),
  },
  {
    id: 'contact',
    heading: 'Contact',
    body: (
      <>
        <LegalParagraph>
          Questions, corrections or requests concerning this policy or the information CampusCore holds can be sent
          to <a href={`mailto:${PRIVACY_CONTACT_EMAIL}`}>{PRIVACY_CONTACT_EMAIL}</a>. Please include the username or
          record concerned, the role you sign in with, and a short description of the request. Do not send passwords
          or tokens by email.
        </LegalParagraph>
      </>
    ),
  },
]

export function PrivacyPolicyPage() {
  return (
    <LegalDocument
      title="Privacy Policy"
      summary="How CampusCore handles the account, profile and academic information it is given, and who can see it."
      sections={sections}
    />
  )
}