﻿/*
 * SonarAnalyzer for .NET
 * Copyright (C) 2015-2023 SonarSource SA
 * mailto: contact AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

namespace SonarAnalyzer.Rules.CSharp
{
    [DiagnosticAnalyzer(LanguageNames.CSharp)]
    public sealed class NoExceptionsInFinally : NoExceptionsInFinallyBase<SyntaxKind>
    {
        protected override ILanguageFacade<SyntaxKind> Language => CSharpFacade.Instance;

        protected override void Initialize(SonarAnalysisContext context) =>
            context.RegisterNodeAction(
                c => new ThrowInFinallyWalker(c, Rule).SafeVisit(((FinallyClauseSyntax)c.Node).Block),
                SyntaxKind.FinallyClause);

        private sealed class ThrowInFinallyWalker : SafeCSharpSyntaxWalker
        {
            private readonly SonarSyntaxNodeReportingContext context;
            private readonly DiagnosticDescriptor rule;

            public ThrowInFinallyWalker(SonarSyntaxNodeReportingContext context, DiagnosticDescriptor rule)
            {
                this.context = context;
                this.rule = rule;
            }

            public override void VisitThrowStatement(ThrowStatementSyntax node) =>
                context.ReportIssue(Diagnostic.Create(rule, node.GetLocation()));

            public override void VisitFinallyClause(FinallyClauseSyntax node)
            {
                // Do not call base to force the walker to stop. Another walker will take care of this finally clause.
            }
        }
    }
}
