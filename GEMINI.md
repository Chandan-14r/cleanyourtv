# Aurora Sapphire - CleanYourTV Design & Interaction Guidelines

## Emil Kowalski Design Engineering & Motion Guidelines

### Mandatory Protocol for UI/UX & Motion
Before building or modifying any UI/UX, screen layout, motion, or interaction:
1. **Always Consult Emil Kowalski's Design Skills:**
   - Reference `emil-design-eng`, `animate`, `apple-design`, `review-animations`, `animation-performance`, and `animation-vocabulary`.
   - Never use `ease-in` for entrance animations; always use responsive `ease-out` or tuned spring physics.
   - For entering elements: use responsive `ease-out` curves or well-damped springs (`dampingRatio >= 0.85`, settling without wobbling).
   - For button/card presses: always provide tactile feedback (e.g. `0.96x` scale dip).
   - Keep all transitions strictly GPU-accelerated (`translationX/Y/Z`, `scaleX/Y`, `alpha`, hardware layers) to guarantee locked 60 FPS on Fire TV hardware (MediaTek MT8695).
   - Use subtle, physical properties: no oversized scaling, no harsh neon outlines, no distracting continuous loops.
2. **Proactive Suggestion & Alignment:**
   - Before implementing design updates, present structured suggestions using Emil Kowalski's Before / After / Why format.
   - Solicit the user's feedback and design preferences before finalizing code changes.
