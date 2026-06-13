export type Role = 'parent' | 'tutor' | 'admin' | 'moderator';

export type Subject = {
  slug: string;
  title: string;
  description: string;
  tutorCount: number | null;
  accent: string;
};

export type TutorSummary = {
  id: string;
  slug: string;
  name: string;
  city: string;
  experienceYears: number;
  rating: number;
  reviewCount: number;
  hourlyRate: number;
  subjects: string[];
  tags: string[];
  verified: boolean;
  featured: boolean;
  nextAvailable: string;
  about: string;
  avatarTone: string;
  primarySubjectSlug: string;
};

export type Review = {
  id: string;
  author: string;
  date: string;
  rating: number;
  text: string;
  lesson: string;
};

export type ReviewEligibility = {
  eligible: boolean;
  reason: string;
  existingReview: Review | null;
};

export type TutorProfile = TutorSummary & {
  education: string[];
  highlights: string[];
  schedule: string[];
  reviews: Review[];
  stats: {
    responseTime: string;
    reviews: number;
    successRate: string;
    activeStudents: number | null;
    averageRating: number;
    lessons?: number | null;
    repeatRate?: string;
  };
};

export type HeroMetric = {
  label: string;
  value: string;
  tone: 'indigo' | 'emerald' | 'amber';
};

export type ProcessStep = {
  title: string;
  text: string;
};

export type FaqItem = {
  question: string;
  answer: string;
};

export type Testimonial = {
  author: string;
  role: string;
  text: string;
};

export type HomeContent = {
  metrics: HeroMetric[];
  process: ProcessStep[];
  testimonials: Testimonial[];
  faq: FaqItem[];
};

export type TutorFilters = {
  query?: string;
  city?: string;
  subject?: string;
  minRating?: number;
  maxPrice?: number;
  onlineOnly?: boolean;
};

export type SignInInput = {
  email: string;
  password: string;
};

export type SignUpParentInput = {
  name: string;
  email: string;
  password: string;
};

export type SignUpTutorInput = {
  name: string;
  email: string;
  password: string;
  subjects: string[];
  experienceYears: string;
  city: string;
  about: string;
};

export type AuthSession = {
  token: string;
  refreshToken?: string;
  user: {
    id: string;
    name: string;
    email: string;
    role: Role;
  };
};

export type ClientApplicationsPageData = {
  metrics: Array<{
    value: string;
    label: string;
    tone: 'indigo' | 'emerald' | 'violet' | 'orange';
  }>;
  tabs: string[];
  applications: Array<{
    id: string;
    initials: string;
    name: string;
    subject: string;
    chips: string[];
    status: string;
    statusTone: 'green' | 'amber' | 'indigo' | 'slate';
    price: string;
    note: string;
    action: string;
  }>;
  nextLesson: {
    title: string;
    pill: string;
    teacher: string;
    text: string;
  };
  matchProgress: Array<{
    label: string;
    value: number;
  }>;
  recommendations: string[];
};

export type ClientOverviewPageData = {
  activeApplications: number;
  recentApplications: Array<{
    id: string;
    tutorName: string;
    tutorSlug: string;
    subject: string;
    status: string;
    statusTone: 'green' | 'amber' | 'indigo' | 'slate';
  }>;
  nextLesson: {
    id: string;
    applicationId: string;
    title: string;
    tutorName: string;
    subject: string;
    format: string;
    startsAt: string;
  } | null;
  recentConversation: {
    threadId: string;
    participantName: string;
    subject: string;
    preview: string;
    updatedAt: string;
  } | null;
};

export type ClientApplicationDetailsData = {
  id: string;
  tutorName: string;
  tutorSlug: string;
  status: string;
  statusTone: 'green' | 'amber' | 'indigo' | 'slate';
  subject: string;
  chips: string[];
  note: string;
  price: string;
  parameters: Array<{
    label: string;
    value: string;
  }>;
};

export type ClientSchedulePageData = {
  metrics: Array<{
    icon: string;
    value: string;
    label: string;
    caption: string;
    tone: 'indigo' | 'violet' | 'amber' | 'emerald';
  }>;
  period: {
    from: string;
    to: string;
    label: string;
  };
  weekDays: Array<{
    day: string;
    date: string;
    active?: boolean;
  }>;
  timeSlots: string[];
  events: Array<{
    id: string;
    lessonId: string;
    applicationId?: string | null;
    threadId?: string | null;
    title: string;
    meta: string;
    time: string;
    startsAt: string;
    endsAt: string;
    tone: 'indigo' | 'violet' | 'blue' | 'green' | 'orange';
    column: number;
    row: number;
    canJoin: boolean;
    canReschedule: boolean;
    canMessageTutor: boolean;
  }>;
  rescheduleRequests: Array<{
    id: string;
    lessonId: string;
    time: string;
    title: string;
    status: string;
    tone: 'orange' | 'blue';
  }>;
  miniMonthDays: Array<{
    date: string;
    active?: boolean;
  }>;
  upcomingLessons: Array<{
    id: string;
    lessonId: string;
    applicationId?: string | null;
    threadId?: string | null;
    time: string;
    title: string;
    details: string;
    canJoin: boolean;
    canReschedule: boolean;
    canMessageTutor: boolean;
  }>;
  emptyState: {
    show: boolean;
    title: string;
    text: string;
  };
};

export type ClientMessagesPageData = {
  metrics: Array<{
    value: string;
    label: string;
    tone: 'indigo' | 'amber' | 'emerald';
  }>;
  filters: string[];
  conversations: Array<{
    id: string;
    name: string;
    initials: string;
    subject: string;
    preview: string;
    time: string;
    unread: number;
    active?: boolean;
  }>;
  activeDialog: {
    threadId?: string;
    name: string;
    initials: string;
    subtitle: string;
    tutorSlug?: string;
    applicationId?: string;
  };
  messages: Array<{
    id?: string;
    author: string;
    time: string;
    text: string;
    own?: boolean;
  }>;
  lessonDetails: Array<{
    label: string;
    value: string;
  }>;
  quickActions: string[];
  attachments: Array<{
    name: string;
    meta: string;
  }>;
};

export type ClientSettings = {
  metrics: Array<{
    icon: string;
    value: string;
    label: string;
    sublabel: string;
    tone: 'emerald' | 'indigo' | 'amber' | 'violet';
  }>;
  profile: {
    parentName: string;
    phone: string;
    email: string;
    city: string;
    timezone: string;
    language: string;
  };
  students: Array<{
    id: string;
    name: string;
    details: string;
    goal: string;
    tone: 'indigo' | 'violet' | 'emerald';
  }>;
  notifications: Array<{
    id: string;
    title: string;
    description: string;
    enabled: boolean;
  }>;
  privacy: Array<{
    id: string;
    title: string;
    description: string;
    enabled: boolean;
  }>;
  searchPreferences: string[];
};

export type ClientFavoritesPageData = {
  tutors: Array<{
    id: string;
    slug: string;
    name: string;
    subject: string;
    meta: string;
    rating: string;
    price: string;
    savedAt: string;
  }>;
};

export type ClientHistoryPageData = {
  stats: Array<{
    icon: string;
    value: string;
    label: string;
    sublabel: string;
    tone: 'indigo' | 'emerald' | 'amber' | 'violet';
  }>;
  filters: string[];
  activeMonth: string;
  events: Array<{
    id: string;
    type: 'lesson' | 'application';
    date: string;
    title: string;
    text: string;
    status: string;
    tone: 'emerald' | 'indigo' | 'violet' | 'amber' | 'slate';
  }>;
  monthSummary: Array<{
    title: string;
    caption: string;
    value: string;
    tone: 'emerald' | 'indigo' | 'violet';
  }>;
  completedLessons: Array<{
    lessonId: string;
    date: string;
    tutor: string;
    subject: string;
    result: string;
    href: string;
  }>;
  emptyState: {
    show: boolean;
    title: string;
    text: string;
  };
};

export type TutorRequestsPageData = {
  stats: Array<{
    icon: string;
    value: string;
    label: string;
    sublabel: string;
    tone: 'indigo' | 'cyan' | 'emerald' | 'amber';
  }>;
  requests: Array<{
    id: string;
    initials: string;
    studentName: string;
    gradeGoal: string;
    subject: string;
    status: string;
    description: string;
    price: string;
    format: string;
    preferredTime: string;
    tone: 'indigo' | 'emerald' | 'amber';
    highlighted?: boolean;
    stage?: TutorRequestStage;
    source?: string;
    deadline?: string;
    overdue?: boolean;
    leadScore?: number;
    nextAction?: string;
    rejectionReason?: string | null;
    threadId?: string | null;
    scheduledLessonId?: string | null;
    scheduledLessonAt?: string | null;
    completenessLabel?: string;
  }>;
  today: {
    title: string;
    summary: string;
    events: Array<{
      time: string;
      text: string;
      tone: 'indigo' | 'emerald' | 'amber';
    }>;
  };
  quality: {
    label: string;
    value: string;
    text: string;
    percent: number;
  };
  funnel?: {
    newCount: number;
    viewedCount: number;
    respondedCount: number;
    contactCount: number;
    trialCount: number;
    studentCount: number;
    closedCount: number;
    trialConversionPercent: number;
    studentConversionPercent: number;
    overdueCount: number;
    averageResponseTime: string;
  };
};

export type TutorRequestStage =
  | 'NEW'
  | 'VIEWED'
  | 'ACCEPTED'
  | 'CONTACT_ESTABLISHED'
  | 'TRIAL_SCHEDULED'
  | 'STUDENT'
  | 'REJECTED'
  | 'CANCELLED'
  | 'ARCHIVED';

export type TutorLessonCandidate = {
  applicationId: string;
  studentId: string;
  studentName: string;
  subject: string;
  format: string;
  desiredStartTime: string;
  gradeGoal: string;
};

export type TutorCalendarSlot = {
  date: string;
  startTime: string;
  endTime: string;
  available: boolean;
  source: 'RULE' | 'EXTRA';
};

export type TutorCalendarPageData = {
  stats: Array<{
    icon: string;
    value: string;
    label: string;
    sublabel: string;
    tone: 'indigo' | 'cyan' | 'emerald' | 'amber';
  }>;
  weekDays: Array<{
    day: string;
    date: string;
    active?: boolean;
  }>;
  timeSlots: string[];
  events: Array<{
    id: string;
    title: string;
    subtitle: string;
    tone: 'indigo' | 'emerald' | 'slate' | 'amber';
    column: number;
    row: number;
    rows?: number;
    startsAt?: string;
    seriesId?: string | null;
    recurrenceLabel?: string;
    applicationId?: string | null;
    threadId?: string | null;
    studentId?: string | null;
  }>;
  nextLesson: {
    time: string;
    title: string;
    description: string;
  };
  upcomingEvents: Array<{
    time: string;
    text: string;
    tone: 'indigo' | 'emerald' | 'amber';
  }>;
  monthDays: Array<{
    day: string;
    muted?: boolean;
    selected?: boolean;
  }>;
  slots: TutorCalendarSlot[];
  pendingRequests: Array<{
    applicationId: string;
    studentId: string;
    studentName: string;
    subject: string;
    format?: string;
    desiredStartTime: string;
    gradeGoal: string;
    threadId?: string | null;
  }>;
};

export type TutorOverviewPageData = {
  newRequests: number;
  activeStudents: number;
  upcomingLessons: number;
  nextLesson: {
    id: string;
    applicationId: string;
    studentName: string;
    title: string;
    subject: string;
    format: string;
    startsAt: string;
  } | null;
};

export type AvailabilitySchedule = {
  id: string;
  dayOfWeek: number;
  startTime: string;
  endTime: string;
  slotDurationMinutes: number;
  subjectId?: string | null;
  active: boolean;
  validUntil?: string | null;
};

export type SlotOverride = {
  id: string;
  date: string;
  startTime: string;
  durationMinutes: number;
  type: 'BLOCKED' | 'EXTRA';
};

export type TutorStudentsPageData = {
  stats: Array<{
    icon: string;
    value: string;
    label: string;
    sublabel: string;
    tone: 'indigo' | 'emerald' | 'amber' | 'cyan';
  }>;
  students: Array<{
    id: string;
    initials: string;
    name: string;
    details: string;
    subject: string;
    status: string;
    note: string;
    progress: number;
    tone: 'indigo' | 'emerald' | 'amber';
    highlighted?: boolean;
  }>;
  selectedStudent: {
    initials: string;
    name: string;
    details: string;
    badges: string[];
    progress: number;
    tasks: Array<{
      title: string;
      meta: string;
      tone: 'indigo' | 'emerald' | 'amber';
    }>;
  };
  cards: {
    homework: {
      title: string;
      text: string;
    };
    payments: {
      label: string;
      value: string;
      text: string;
    };
    reports: {
      title: string;
      text: string;
    };
  };
};

export type TutorWorkspaceProfilePageData = {
  stats: Array<{
    icon: string;
    value: string;
    label: string;
    sublabel: string;
    tone: 'indigo' | 'emerald' | 'amber' | 'cyan';
  }>;
  identity: {
    name: string;
    role: string;
    city: string;
    avatarInitials: string;
    verified: boolean;
    status: string;
    completion: number;
    experienceYears?: number;
    pricePerHour?: number;
  };
  publicCard: {
    title: string;
    subtitle: string;
    rating: string;
    price: string;
    response: string;
    views: string;
    chips: string[];
  };
  fields: Array<{
    id: string;
    label: string;
    value: string;
    type: 'input' | 'textarea';
  }>;
  subjects: Array<{
    title: string;
    meta: string;
    price: string;
    tone: 'indigo' | 'emerald' | 'amber';
  }>;
  checklist: Array<{
    title: string;
    text: string;
    done: boolean;
  }>;
};

export type TutorMessagesPageData = {
  metrics: Array<{
    value: string;
    label: string;
    tone: 'indigo' | 'emerald' | 'amber';
  }>;
  conversations: Array<{
    id: string;
    name: string;
    initials: string;
    subject: string;
    preview: string;
    time: string;
    unread: number;
    active?: boolean;
  }>;
  activeDialog: {
    threadId?: string;
    initials: string;
    name: string;
    subtitle: string;
    requestId?: string;
    studentId?: string;
  };
  messages: Array<{
    id?: string;
    author: string;
    time: string;
    text: string;
    own?: boolean;
  }>;
  details: Array<{
    label: string;
    value: string;
  }>;
  quickActions: string[];
  materials: Array<{
    name: string;
    meta: string;
  }>;
};

export type TutorSubscriptionPageData = {
  plan: {
    name: string;
    status: string;
    price: string;
    period: string;
    expiresAt: string;
    nextPayment: string;
    usagePercent: number;
  };
  metrics: Array<{
    icon: string;
    value: string;
    label: string;
    sublabel: string;
    tone: 'indigo' | 'emerald' | 'amber' | 'cyan';
  }>;
  features: Array<{
    title: string;
    text: string;
    enabled: boolean;
  }>;
  tariffs: Array<{
    id: string;
    name: string;
    price: string;
    period: string;
    description: string;
    badge?: string;
    current?: boolean;
    features: string[];
  }>;
  history: Array<{
    date: string;
    title: string;
    amount: string;
    status: string;
  }>;
  recommendation: {
    title: string;
    text: string;
    action: string;
  };
};

export type TutorTariffCheckoutPageData = {
  status: string;
  summary: Array<{
    icon: string;
    value: string;
    label: string;
    tone: 'indigo' | 'emerald' | 'amber' | 'cyan';
  }>;
  qr: {
    title: string;
    hint: string;
    timer: string;
    amount: string;
    footnote: string;
  };
  order: Array<{
    label: string;
    value: string;
  }>;
  timeline: Array<{
    title: string;
    hint: string;
    done?: boolean;
  }>;
  activation: Array<{
    title: string;
    text: string;
  }>;
};

export type TutorSettingsPageData = {
  profile: {
    name: string;
    city: string;
    headline: string;
    about: string;
    initials: string;
    moderationStatus: string;
    experienceYears?: number;
    pricePerHour?: number;
  };
  subjects: string[];
  availableSubjects: string[];
  isHidden: boolean;
  visibility: Array<{
    title: string;
    description: string;
    enabled: boolean;
  }>;
  notifications: Array<{
    title: string;
    description: string;
    enabled: boolean;
  }>;
  moderation: {
    status: string;
    description: string;
  };
};

export type AdminDashboardPageData = {
  metrics: Array<{
    icon: string;
    label: string;
    value: string;
    delta: string;
    tone: 'teal' | 'indigo' | 'amber' | 'rose';
  }>;
  services: Array<{
    name: string;
    status: string;
    uptime: string;
    latency: string;
    tone: 'green' | 'amber' | 'red';
  }>;
  alerts: Array<{
    id: string;
    title: string;
    description: string;
    severity: 'high' | 'medium' | 'low';
    time: string;
  }>;
  moderationQueue: Array<{
    id: string;
    type: string;
    target: string;
    status: string;
    owner: string;
  }>;
  quickActions: Array<{
    title: string;
    text: string;
    to: string;
  }>;
  audit: Array<{
    id: string;
    actor: string;
    action: string;
    date: string;
  }>;
};

export type ModeratorComplaintsPageData = {
  metrics: Array<{
    icon: string;
    value: string;
    label: string;
    hint: string;
    trend: string;
    tone: 'rose' | 'amber' | 'emerald' | 'indigo';
  }>;
  tabs: string[];
  complaints: Array<{
    id: string;
    initials: string;
    direction: string;
    category: string;
    status: string;
    subject: string;
    summary: string;
    time: string;
    evidence: string;
    owner: string;
    tone: 'rose' | 'amber' | 'indigo' | 'slate';
    active?: boolean;
  }>;
  selected: {
    id: string;
    status: string;
    subject: string;
    copy: string;
    participants: Array<{
      initials: string;
      name: string;
      role: string;
    }>;
    evidence: Array<{
      label: string;
      ready: boolean;
    }>;
    impact: {
      label: string;
      hint: string;
    };
    actions: string[];
  };
  selectedById?: Record<string, ModeratorComplaintsPageData['selected']>;
  sla: Array<{
    label: string;
    value: string;
    percent: number;
  }>;
  weekly: {
    bars: number[];
    labels: string[];
    hint: string;
  };
  rules: {
    title: string;
    body: string;
    tags: string[];
  };
};

export type ModeratorReviewModerationPageData = {
  metrics: Array<{
    icon: string;
    value: string;
    label: string;
    hint: string;
    trend: string;
    tone: 'rose' | 'amber' | 'emerald' | 'indigo';
  }>;
  tabs: string[];
  profiles: Array<{
    id: string;
    itemType: 'REVIEW' | 'TUTOR_PROFILE';
    initials: string;
    name: string;
    subject: string;
    type: string;
    status: string;
    summary: string;
    score: string;
    owner: string;
    tone: 'rose' | 'amber' | 'indigo' | 'emerald';
    active?: boolean;
  }>;
  selected: {
    id: string;
    itemType?: 'REVIEW' | 'TUTOR_PROFILE';
    status: string;
    initials: string;
    name: string;
    subject: string;
    experience: string;
    risk: string;
    checklist: Array<{
      label: string;
      done: boolean;
    }>;
    score: string;
    note: string;
  };
  pipeline: Array<{
    label: string;
    value: string;
    percent: number;
  }>;
  rubric: string[];
  decision: {
    title: string;
    body: string;
    tags: string[];
  };
};

export type ModeratorDashboardPageData = {
  pendingComplaints: number;
  pendingReviews: number;
  pendingProfiles: number;
  totalPending: number;
  recentDecisions: Array<{
    id: string;
    action: string;
    targetType: string;
    targetId: string;
    decidedAt: string;
  }>;
};

export type AdminUsersPageData = {
  metrics: Array<{
    icon: string;
    value: string;
    title: string;
    meta: string;
    tone: 'indigo' | 'emerald' | 'amber' | 'rose';
  }>;
  tabs: string[];
  users: Array<{
    id: string;
    initials: string;
    name: string;
    email: string;
    role: string;
    status: string;
    risk: 'Low' | 'Med' | 'High';
    tone: 'indigo' | 'emerald' | 'amber' | 'rose';
    active?: boolean;
  }>;
  selected: {
    initials: string;
    name: string;
    email: string;
    role: string;
    status: string;
    riskTitle: string;
    signals: Array<{
      title: string;
      meta: string;
    }>;
    actions: string[];
  };
  cards: {
    audit: {
      title: string;
      text: string;
      action: string;
    };
    roles: {
      label: string;
      value: string;
      text: string;
      action: string;
    };
    moderation: {
      title: string;
      text: string;
      action: string;
    };
    note: {
      title: string;
      text: string;
      action: string;
    };
  };
};

export type AdminSiteStatsPageData = {
  metrics: Array<{
    icon: string;
    value: string;
    title: string;
    meta: string;
    tone: 'indigo' | 'emerald' | 'cyan' | 'amber';
  }>;
  tabs: string[];
  growth: {
    title: string;
    caption: string;
    labels: string[];
    users: number[];
    requests: number[];
    footer: string;
  };
  conversion: {
    label: string;
    value: string;
    text: string;
  };
  quality: Array<{
    label: string;
    value: string;
    percent: number;
  }>;
  funnel: Array<{
    label: string;
    value: string;
    percent: number;
  }>;
  finance: {
    title: string;
    value: string;
    caption: string;
    trend: string;
  };
  geo: Array<{
    city: string;
    value: string;
    percent: number;
  }>;
  events: Array<{
    time: string;
    title: string;
    meta: string;
    tone: 'emerald' | 'amber' | 'indigo';
  }>;
  note: {
    title: string;
    text: string;
    action: string;
  };
};

export type AdminSubscriptionsPageData = {
  metrics: Array<{
    icon: string;
    value: string;
    label: string;
    hint: string;
    tone: 'emerald' | 'violet' | 'indigo' | 'rose';
  }>;
  subscriptions: Array<{
    id: string;
    tutor: string;
    plan: 'Free' | 'CRM Basic' | 'CRM Pro' | 'CRM Premium';
    status: string;
    expiresAt: string;
    payment: string;
    tone: 'emerald' | 'slate' | 'amber' | 'rose';
  }>;
  revenueByPlan: Array<{
    plan: 'CRM Basic' | 'CRM Pro' | 'CRM Premium';
    value: string;
    percent: number;
    tone: 'slate' | 'indigo' | 'violet';
  }>;
  webhookQueue: Array<{
    id: string;
    event: string;
    action: string;
    tone: 'indigo' | 'amber' | 'rose';
  }>;
  planSettings: Array<{
    name: 'CRM Basic' | 'CRM Pro' | 'CRM Premium';
    price: string;
    body: string;
  }>;
};

export type AdminAuditPageData = {
  metrics: Array<{
    icon: string;
    value: string;
    label: string;
    hint: string;
    tone: 'indigo' | 'emerald' | 'amber' | 'rose';
  }>;
  tabs: string[];
  events: Array<{
    id: string;
    time: string;
    actor: string;
    role: string;
    action: string;
    target: string;
    service: string;
    severity: 'low' | 'medium' | 'high';
    status: string;
    tone: 'indigo' | 'emerald' | 'amber' | 'rose';
    active?: boolean;
  }>;
  selected: {
    id: string;
    title: string;
    actor: string;
    description: string;
    meta: Array<{
      label: string;
      value: string;
    }>;
    payload: string[];
  };
  policies: Array<{
    title: string;
    text: string;
    percent: number;
  }>;
  exports: Array<{
    title: string;
    text: string;
    action: string;
  }>;
};

export type AdminSettingsPageData = {
  metrics: Array<{
    icon: string;
    value: string;
    label: string;
    hint: string;
    tone: 'indigo' | 'emerald' | 'amber' | 'rose';
  }>;
  security: Array<{
    title: string;
    description: string;
    enabled: boolean;
  }>;
  roles: Array<{
    title: string;
    description: string;
    users: string;
  }>;
  limits: Array<{
    label: string;
    value: string;
    description: string;
  }>;
  services: Array<{
    name: string;
    status: string;
    description: string;
    tone: 'emerald' | 'amber' | 'rose';
  }>;
  policies: Array<{
    title: string;
    text: string;
    tags: string[];
  }>;
};

export type RoadmapApiStub = {
  method: 'GET' | 'POST' | 'PATCH' | 'DELETE';
  path: string;
  description: string;
};

export type RoadmapRoute = {
  index?: boolean;
  path?: string;
  title: string;
  description: string;
  api: RoadmapApiStub[];
};

export type RoadmapSection = {
  key: 'client' | 'tutor' | 'admin' | 'moderator';
  title: string;
  summary: string;
  accent: string;
  routes: RoadmapRoute[];
};
